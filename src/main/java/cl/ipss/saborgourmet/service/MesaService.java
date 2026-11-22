package cl.ipss.saborgourmet.service;

import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.repository.MesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logica de negocio relacionada con las mesas del restaurante.
 *
 * La anotacion @Service marca la clase como un componente de la capa de negocio.
 * Spring la detecta al iniciar, crea una unica instancia y la inyecta donde se
 * necesite. Esta capa existe para que los controladores no hablen directamente
 * con los repositorios: aqui viven las reglas, alli solo el acceso a datos.
 */
@Service
public class MesaService {

    private final MesaRepository mesaRepository;

    /**
     * Inyeccion de dependencias por constructor.
     *
     * Spring entrega automaticamente la implementacion del repositorio. Se
     * prefiere el constructor sobre @Autowired en un atributo porque deja la
     * dependencia como final (no puede cambiar despues) y permite crear la clase
     * manualmente en las pruebas unitarias.
     */
    public MesaService(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
    }

    /** Todas las mesas ordenadas por numero, para la vista de administracion. */
    @Transactional(readOnly = true)
    public List<Mesa> listarTodas() {
        return mesaRepository.findAllByOrderByNumeroAsc();
    }

    /** Busca una mesa por su identificador o lanza una excepcion si no existe. */
    @Transactional(readOnly = true)
    public Mesa obtener(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("La mesa indicada no existe."));
    }

    /** Cantidad de mesas habilitadas. Se muestra en el panel del personal. */
    @Transactional(readOnly = true)
    public long contarActivas() {
        return mesaRepository.countByActivaTrue();
    }

    /**
     * Crea o actualiza una mesa.
     *
     * Antes de guardar valida que el numero no este tomado por otra mesa. La
     * comprobacion se hace aqui, ademas de la restriccion unique de la base de
     * datos, para poder mostrar al usuario un mensaje claro en lugar de un error
     * tecnico de SQL.
     *
     * @Transactional agrupa la lectura y la escritura en una sola transaccion:
     * si algo falla, no queda nada a medio guardar.
     */
    @Transactional
    public Mesa guardar(Mesa mesa) {
        mesaRepository.findByNumero(mesa.getNumero()).ifPresent(existente -> {
            boolean esOtraMesa = mesa.getId() == null || !existente.getId().equals(mesa.getId());
            if (esOtraMesa) {
                throw new ReglaNegocioException("Ya existe una mesa con el numero " + mesa.getNumero() + ".");
            }
        });
        return mesaRepository.save(mesa);
    }

    /**
     * Habilita o deshabilita una mesa.
     *
     * No se elimina el registro para conservar el historial de reservas que la
     * referencian. Una mesa inactiva simplemente deja de ofrecerse.
     */
    @Transactional
    public Mesa cambiarDisponibilidad(Long id) {
        Mesa mesa = obtener(id);
        mesa.setActiva(!mesa.isActiva());
        return mesaRepository.save(mesa);
    }
}
