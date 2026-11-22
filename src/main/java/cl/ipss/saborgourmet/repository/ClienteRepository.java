package cl.ipss.saborgourmet.repository;

import cl.ipss.saborgourmet.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de clientes. Hereda de JpaRepository todas las operaciones CRUD.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Busca un cliente por su correo, ignorando mayusculas y minusculas.
     *
     * Devuelve Optional porque el cliente puede no existir todavia. El servicio
     * lo usa para reutilizar la ficha de un cliente que ya reservo antes en
     * lugar de crear un registro duplicado.
     */
    Optional<Cliente> findByEmailIgnoreCase(String email);
}
