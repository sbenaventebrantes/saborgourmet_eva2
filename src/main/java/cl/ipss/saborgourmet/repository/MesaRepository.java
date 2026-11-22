package cl.ipss.saborgourmet.repository;

import cl.ipss.saborgourmet.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de mesas.
 *
 * Al extender JpaRepository, Spring Data genera en tiempo de ejecucion una clase
 * que implementa esta interfaz, con las operaciones CRUD ya resueltas: save,
 * findById, findAll, deleteById y otras. No hay que escribir SQL ni implementar
 * nada: basta declarar los metodos siguiendo la convencion de nombres y Spring
 * Data deduce la consulta a partir del nombre del metodo.
 *
 * Los dos parametros genericos son el tipo de la entidad (Mesa) y el tipo de su
 * clave primaria (Long).
 */
public interface MesaRepository extends JpaRepository<Mesa, Long> {

    /** Mesas habilitadas con capacidad suficiente, ordenadas de menor a mayor. */
    List<Mesa> findByActivaTrueAndCapacidadGreaterThanEqualOrderByCapacidadAscNumeroAsc(Integer capacidad);

    /** Todas las mesas ordenadas por su numero, para el listado de administracion. */
    List<Mesa> findAllByOrderByNumeroAsc();

    /** Busca por el numero visible de la mesa; se usa para evitar duplicados. */
    Optional<Mesa> findByNumero(Integer numero);

    /** Cantidad de mesas habilitadas. Alimenta el panel de administracion. */
    long countByActivaTrue();
}
