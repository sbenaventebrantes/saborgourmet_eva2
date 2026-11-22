package cl.ipss.saborgourmet.service;

import cl.ipss.saborgourmet.dto.ReservaForm;
import cl.ipss.saborgourmet.model.Cliente;
import cl.ipss.saborgourmet.model.EstadoReserva;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;
import cl.ipss.saborgourmet.repository.ClienteRepository;
import cl.ipss.saborgourmet.repository.MesaRepository;
import cl.ipss.saborgourmet.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Corazon del sistema: concentra todas las reglas de negocio de las reservas.
 *
 * Los controladores solo reciben datos y eligen la vista; toda decision sobre
 * que es una reserva valida se toma aqui. Esto mantiene separada la capa de
 * presentacion de la logica, que es uno de los objetivos del patron MVC.
 */
@Service
public class ReservaService {

    /** Caracteres permitidos en el codigo publico. Se omiten letras y numeros
     *  que se confunden al dictarlos por telefono (O, 0, I, 1). */
    private static final String CARACTERES_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final ReservaRepository reservaRepository;
    private final MesaRepository mesaRepository;
    private final ClienteRepository clienteRepository;

    /**
     * Turnos que atiende el restaurante. Se leen desde application.yml con
     * @Value, de modo que el horario se puede cambiar sin tocar el codigo.
     */
    private final List<LocalTime> horarios;

    /** Dias hacia adelante que se pueden reservar, tambien configurable. */
    private final int diasAnticipacion;

    public ReservaService(ReservaRepository reservaRepository,
                          MesaRepository mesaRepository,
                          ClienteRepository clienteRepository,
                          @Value("${restaurante.horarios}") List<String> horariosConfigurados,
                          @Value("${restaurante.dias-anticipacion}") int diasAnticipacion) {
        this.reservaRepository = reservaRepository;
        this.mesaRepository = mesaRepository;
        this.clienteRepository = clienteRepository;
        this.diasAnticipacion = diasAnticipacion;
        // Convierte los textos "13:00", "14:30"... en objetos LocalTime.
        this.horarios = horariosConfigurados.stream().map(LocalTime::parse).toList();
    }

    /** Turnos disponibles. Las vistas los recorren para armar el desplegable. */
    public List<LocalTime> getHorarios() {
        return horarios;
    }

    /** Ultimo dia que se puede reservar, usado como limite del campo de fecha. */
    public LocalDate getFechaMaxima() {
        return LocalDate.now().plusDays(diasAnticipacion);
    }

    /**
     * Devuelve las mesas libres para una fecha, hora y cantidad de personas.
     *
     * El calculo tiene tres pasos:
     *  1. Validar que la fecha y la hora sean posibles.
     *  2. Pedir al repositorio las mesas habilitadas con capacidad suficiente.
     *  3. Descartar las que ya tienen una reserva confirmada en ese turno.
     *
     * Se consulta una sola vez la lista de mesas ocupadas en lugar de preguntar
     * por cada mesa, para evitar multiples consultas innecesarias.
     */
    @Transactional(readOnly = true)
    public List<Mesa> buscarMesasDisponibles(LocalDate fecha, LocalTime hora, int personas) {
        validarFechaYHora(fecha, hora);

        List<Mesa> candidatas = mesaRepository
                .findByActivaTrueAndCapacidadGreaterThanEqualOrderByCapacidadAscNumeroAsc(personas);

        List<Long> ocupadas = reservaRepository.buscarIdsDeMesasOcupadas(fecha, hora, EstadoReserva.CONFIRMADA);

        List<Mesa> disponibles = new ArrayList<>();
        for (Mesa mesa : candidatas) {
            if (!ocupadas.contains(mesa.getId())) {
                disponibles.add(mesa);
            }
        }
        return disponibles;
    }

    /**
     * Registra una reserva nueva a partir de los datos del formulario.
     *
     * Todo el metodo corre dentro de una transaccion: si falla cualquiera de las
     * validaciones o el guardado, no se escribe nada en la base de datos.
     */
    @Transactional
    public Reserva crear(ReservaForm form) {
        validarFechaYHora(form.getFecha(), form.getHora());

        Mesa mesa = mesaRepository.findById(form.getMesaId())
                .orElseThrow(() -> new ReglaNegocioException("La mesa seleccionada no existe."));

        // La mesa debe estar habilitada y tener capacidad para el grupo.
        if (!mesa.admite(form.getPersonas())) {
            throw new ReglaNegocioException(
                    "La mesa " + mesa.getNumero() + " no esta disponible para " + form.getPersonas() + " personas.");
        }

        // Verificacion final contra la base de datos. Protege el caso en que dos
        // clientes elijan la misma mesa con segundos de diferencia: el segundo en
        // confirmar recibe este aviso en lugar de duplicar la reserva.
        if (estaOcupada(mesa.getId(), form.getFecha(), form.getHora())) {
            throw new ReglaNegocioException(
                    "Otro cliente acaba de reservar esa mesa. Por favor elija otra.");
        }

        Cliente cliente = obtenerOCrearCliente(form);

        Reserva reserva = new Reserva(
                generarCodigoUnico(), cliente, mesa,
                form.getFecha(), form.getHora(), form.getPersonas(), form.getObservaciones());

        return reservaRepository.save(reserva);
    }

    /**
     * Modifica una reserva existente: cantidad de personas, turno, mesa u
     * observaciones. Se identifica por su codigo publico.
     */
    @Transactional
    public Reserva actualizar(String codigo, ReservaForm form) {
        Reserva reserva = obtenerPorCodigo(codigo);

        if (!reserva.estaActiva()) {
            throw new ReglaNegocioException("La reserva ya fue cancelada y no se puede modificar.");
        }
        if (reserva.esPasada()) {
            throw new ReglaNegocioException("No se puede modificar una reserva que ya ocurrio.");
        }

        validarFechaYHora(form.getFecha(), form.getHora());

        Mesa mesa = mesaRepository.findById(form.getMesaId())
                .orElseThrow(() -> new ReglaNegocioException("La mesa seleccionada no existe."));

        if (!mesa.admite(form.getPersonas())) {
            throw new ReglaNegocioException(
                    "La mesa " + mesa.getNumero() + " no admite " + form.getPersonas() + " personas.");
        }

        // Si cambio de mesa o de turno, hay que comprobar que el nuevo cupo este
        // libre. Si se queda en la misma mesa y turno, no se valida contra si misma.
        boolean cambioDeCupo = !mesa.getId().equals(reserva.getMesa().getId())
                || !form.getFecha().equals(reserva.getFecha())
                || !form.getHora().equals(reserva.getHora());

        if (cambioDeCupo && estaOcupada(mesa.getId(), form.getFecha(), form.getHora())) {
            throw new ReglaNegocioException("Esa mesa ya esta reservada en el horario elegido.");
        }

        reserva.setMesa(mesa);
        reserva.setFecha(form.getFecha());
        reserva.setHora(form.getHora());
        reserva.setCantidadPersonas(form.getPersonas());
        reserva.setObservaciones(form.getObservaciones());

        // Los datos de contacto tambien se pueden corregir desde el formulario.
        Cliente cliente = reserva.getCliente();
        cliente.setNombre(form.getNombre());
        cliente.setTelefono(form.getTelefono());
        clienteRepository.save(cliente);

        return reservaRepository.save(reserva);
    }

    /**
     * Cancela una reserva. No borra la fila: cambia su estado a CANCELADA, con lo
     * que la mesa vuelve a aparecer como disponible en ese turno.
     */
    @Transactional
    public Reserva cancelar(String codigo) {
        Reserva reserva = obtenerPorCodigo(codigo);
        if (!reserva.estaActiva()) {
            throw new ReglaNegocioException("La reserva ya estaba cancelada.");
        }
        reserva.cancelar();
        return reservaRepository.save(reserva);
    }

    /** Busca una reserva por su codigo o informa que no existe. */
    @Transactional(readOnly = true)
    public Reserva obtenerPorCodigo(String codigo) {
        return reservaRepository.findByCodigo(normalizarCodigo(codigo))
                .orElseThrow(() -> new ReglaNegocioException(
                        "No encontramos una reserva con ese codigo."));
    }

    /**
     * Busqueda que hace el cliente desde el sitio: exige el codigo y el correo.
     *
     * Pedir ambos datos evita que alguien pueda ver o cancelar reservas ajenas
     * probando codigos al azar.
     */
    @Transactional(readOnly = true)
    public Reserva buscarParaCliente(String codigo, String email) {
        Reserva reserva = obtenerPorCodigo(codigo);
        if (email == null || !reserva.getCliente().getEmail().equalsIgnoreCase(email.trim())) {
            throw new ReglaNegocioException("El codigo y el correo no coinciden con ninguna reserva.");
        }
        return reserva;
    }

    /** Reservas de un dia, para la pantalla del personal del restaurante. */
    @Transactional(readOnly = true)
    public List<Reserva> reservasDelDia(LocalDate fecha) {
        return reservaRepository.buscarDelDiaConDetalle(fecha);
    }

    /** Cantidad de reservas confirmadas en un dia. Se muestra en el panel. */
    @Transactional(readOnly = true)
    public long contarConfirmadas(LocalDate fecha) {
        return reservaRepository.countByFechaAndEstado(fecha, EstadoReserva.CONFIRMADA);
    }

    /** Cantidad de comensales esperados en un dia, sumando las reservas activas. */
    @Transactional(readOnly = true)
    public int contarComensales(LocalDate fecha) {
        return reservaRepository.sumarComensales(fecha, EstadoReserva.CONFIRMADA);
    }

    // ------------------------------------------------------------------
    // Metodos auxiliares privados
    // ------------------------------------------------------------------

    /** Consulta directa al repositorio para saber si el cupo esta tomado. */
    private boolean estaOcupada(Long mesaId, LocalDate fecha, LocalTime hora) {
        return reservaRepository.existsByMesaIdAndFechaAndHoraAndEstado(
                mesaId, fecha, hora, EstadoReserva.CONFIRMADA);
    }

    /**
     * Reglas sobre cuando se puede reservar:
     *  - la hora debe ser uno de los turnos que atiende el restaurante,
     *  - no se puede reservar en el pasado,
     *  - no se puede reservar mas alla del limite de anticipacion configurado.
     */
    private void validarFechaYHora(LocalDate fecha, LocalTime hora) {
        if (fecha == null || hora == null) {
            throw new ReglaNegocioException("Debe indicar la fecha y el horario.");
        }
        if (!horarios.contains(hora)) {
            throw new ReglaNegocioException("El horario seleccionado no corresponde a un turno de atencion.");
        }
        if (LocalDateTime.of(fecha, hora).isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("No es posible reservar en una fecha u hora que ya paso.");
        }
        if (fecha.isAfter(getFechaMaxima())) {
            throw new ReglaNegocioException(
                    "Solo aceptamos reservas con hasta " + diasAnticipacion + " dias de anticipacion.");
        }
    }

    /**
     * Reutiliza la ficha del cliente si su correo ya existe y, de paso, actualiza
     * su nombre y telefono. Si es la primera vez, crea el registro.
     * Asi se evitan clientes duplicados en la base de datos.
     */
    private Cliente obtenerOCrearCliente(ReservaForm form) {
        String email = form.getEmail().trim();
        Cliente cliente = clienteRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> new Cliente(form.getNombre(), email, form.getTelefono()));
        cliente.setNombre(form.getNombre().trim());
        cliente.setTelefono(form.getTelefono().trim());
        return clienteRepository.save(cliente);
    }

    /**
     * Genera un codigo del tipo "SG-7KD29A" y comprueba contra la base de datos
     * que no exista. El bucle repite el intento en el caso improbable de choque.
     */
    private String generarCodigoUnico() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("SG-");
            for (int i = 0; i < 6; i++) {
                sb.append(CARACTERES_CODIGO.charAt(ALEATORIO.nextInt(CARACTERES_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (reservaRepository.existsByCodigo(codigo));
        return codigo;
    }

    /** Acepta el codigo escrito en minusculas o con espacios sobrantes. */
    private String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase();
    }
}
