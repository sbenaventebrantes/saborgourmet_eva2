package cl.ipss.saborgourmet;

import cl.ipss.saborgourmet.dto.ReservaForm;
import cl.ipss.saborgourmet.model.EstadoReserva;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;
import cl.ipss.saborgourmet.model.Ubicacion;
import cl.ipss.saborgourmet.repository.MesaRepository;
import cl.ipss.saborgourmet.repository.ReservaRepository;
import cl.ipss.saborgourmet.service.ReglaNegocioException;
import cl.ipss.saborgourmet.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de las reglas de negocio de las reservas.
 *
 * @SpringBootTest levanta la aplicacion completa con la base H2 del perfil
 * "test", de modo que se comprueba el comportamiento real del servicio junto a
 * los repositorios y no una simulacion.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReservaServiceTest {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    private Mesa mesaCuatro;
    private LocalDate manana;
    private LocalTime turno;

    /** Antes de cada prueba se crea una mesa limpia y se fija un turno futuro. */
    @BeforeEach
    void prepararDatos() {
        // Las reservas se borran primero porque apuntan a las mesas mediante una
        // clave foranea: eliminar la mesa con reservas asociadas daria error.
        reservaRepository.deleteAll();
        mesaRepository.deleteAll();
        mesaCuatro = mesaRepository.save(new Mesa(101, 4, Ubicacion.SALON));
        manana = LocalDate.now().plusDays(1);
        turno = reservaService.getHorarios().get(0);
    }

    /** Arma un formulario valido para no repetir estas lineas en cada prueba. */
    private ReservaForm formularioDe(Long mesaId, int personas, String email) {
        ReservaForm form = new ReservaForm();
        form.setMesaId(mesaId);
        form.setFecha(manana);
        form.setHora(turno);
        form.setPersonas(personas);
        form.setNombre("Cliente de prueba");
        form.setEmail(email);
        form.setTelefono("+56 9 1111 2222");
        return form;
    }

    @Test
    @DisplayName("Una mesa libre aparece entre las disponibles")
    void mesaLibreApareceComoDisponible() {
        List<Mesa> disponibles = reservaService.buscarMesasDisponibles(manana, turno, 4);
        assertTrue(disponibles.stream().anyMatch(m -> m.getId().equals(mesaCuatro.getId())));
    }

    @Test
    @DisplayName("Una mesa pequena no se ofrece a un grupo mas grande")
    void mesaPequenaNoSeOfreceAGrupoGrande() {
        List<Mesa> disponibles = reservaService.buscarMesasDisponibles(manana, turno, 8);
        assertFalse(disponibles.stream().anyMatch(m -> m.getId().equals(mesaCuatro.getId())));
    }

    @Test
    @DisplayName("Al reservar, la mesa deja de estar disponible en ese turno")
    void alReservarLaMesaDejaDeEstarDisponible() {
        Reserva reserva = reservaService.crear(formularioDe(mesaCuatro.getId(), 4, "ana@example.com"));

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        assertTrue(reserva.getCodigo().startsWith("SG-"));

        List<Mesa> disponibles = reservaService.buscarMesasDisponibles(manana, turno, 4);
        assertFalse(disponibles.stream().anyMatch(m -> m.getId().equals(mesaCuatro.getId())));
    }

    @Test
    @DisplayName("No se puede reservar dos veces la misma mesa en el mismo turno")
    void noSePuedeReservarDosVecesElMismoCupo() {
        reservaService.crear(formularioDe(mesaCuatro.getId(), 4, "ana@example.com"));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> reservaService.crear(formularioDe(mesaCuatro.getId(), 4, "bruno@example.com")));

        assertTrue(error.getMessage().toLowerCase().contains("reserv"));
    }

    @Test
    @DisplayName("Al cancelar, la mesa vuelve a quedar disponible")
    void alCancelarLaMesaVuelveAQuedarDisponible() {
        Reserva reserva = reservaService.crear(formularioDe(mesaCuatro.getId(), 4, "ana@example.com"));

        Reserva cancelada = reservaService.cancelar(reserva.getCodigo());
        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());

        List<Mesa> disponibles = reservaService.buscarMesasDisponibles(manana, turno, 4);
        assertTrue(disponibles.stream().anyMatch(m -> m.getId().equals(mesaCuatro.getId())));
    }

    @Test
    @DisplayName("No se aceptan reservas en fechas pasadas")
    void noSeAceptanReservasEnElPasado() {
        ReservaForm form = formularioDe(mesaCuatro.getId(), 4, "ana@example.com");
        form.setFecha(LocalDate.now().minusDays(1));

        assertThrows(ReglaNegocioException.class, () -> reservaService.crear(form));
    }

    @Test
    @DisplayName("El cliente solo accede a su reserva con el codigo y su correo")
    void clienteAccedeSoloConCodigoYCorreoCorrectos() {
        Reserva reserva = reservaService.crear(formularioDe(mesaCuatro.getId(), 4, "ana@example.com"));

        Reserva encontrada = reservaService.buscarParaCliente(reserva.getCodigo(), "ana@example.com");
        assertEquals(reserva.getId(), encontrada.getId());

        assertThrows(ReglaNegocioException.class,
                () -> reservaService.buscarParaCliente(reserva.getCodigo(), "otro@example.com"));
    }

    @Test
    @DisplayName("Una reserva se puede modificar cambiando la cantidad de personas")
    void sePuedeModificarLaReserva() {
        Reserva reserva = reservaService.crear(formularioDe(mesaCuatro.getId(), 2, "ana@example.com"));

        ReservaForm cambios = formularioDe(mesaCuatro.getId(), 4, "ana@example.com");
        cambios.setObservaciones("Ahora somos cuatro");

        Reserva actualizada = reservaService.actualizar(reserva.getCodigo(), cambios);

        assertEquals(4, actualizada.getCantidadPersonas());
        assertEquals("Ahora somos cuatro", actualizada.getObservaciones());
    }
}
