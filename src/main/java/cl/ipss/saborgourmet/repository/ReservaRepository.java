package cl.ipss.saborgourmet.repository;

import cl.ipss.saborgourmet.model.EstadoReserva;
import cl.ipss.saborgourmet.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de reservas. Concentra todas las consultas que el sistema necesita
 * sobre la tabla "reservas", de modo que ni los servicios ni los controladores
 * manipulen datos directamente.
 *
 * Se combinan las dos formas que ofrece Spring Data JPA:
 *  - Consultas derivadas del nombre del metodo (findByCodigo, existsBy...), que
 *    no requieren escribir nada mas.
 *  - Consultas JPQL escritas con @Query, para los casos en que se necesita algo
 *    que el nombre del metodo no puede expresar, como traer datos relacionados
 *    en una sola consulta o calcular una suma en la base de datos.
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Busca una reserva por su codigo publico, el que recibe el cliente. */
    Optional<Reserva> findByCodigo(String codigo);

    /** Comprueba si un codigo ya existe, para no repetirlo al generar uno nuevo. */
    boolean existsByCodigo(String codigo);

    /**
     * Indica si una mesa ya tiene reserva confirmada en ese turno.
     *
     * Se usa como validacion final antes de guardar, para evitar que dos clientes
     * tomen la misma mesa si envian el formulario casi al mismo tiempo.
     */
    boolean existsByMesaIdAndFechaAndHoraAndEstado(Long mesaId, LocalDate fecha,
                                                   LocalTime hora, EstadoReserva estado);

    /**
     * Identificadores de las mesas ya ocupadas en una fecha y hora concretas.
     *
     * Esta es la consulta clave para calcular la disponibilidad. Se escribe en
     * JPQL y no en SQL porque trabaja sobre entidades y no sobre tablas:
     * r.mesa.id recorre la asociacion declarada en la entidad Reserva.
     *
     * Al recibir el estado como parametro (:estado) solo se consideran las
     * reservas CONFIRMADAS, de modo que una reserva cancelada libera
     * automaticamente la mesa en ese turno.
     *
     * Devuelve solo los identificadores, no las reservas completas, porque es lo
     * unico que el calculo necesita.
     */
    @Query("""
           SELECT r.mesa.id FROM Reserva r
           WHERE r.fecha = :fecha
             AND r.hora = :hora
             AND r.estado = :estado
           """)
    List<Long> buscarIdsDeMesasOcupadas(@Param("fecha") LocalDate fecha,
                                        @Param("hora") LocalTime hora,
                                        @Param("estado") EstadoReserva estado);

    /**
     * Reservas de un dia con su cliente y su mesa ya cargados.
     *
     * JOIN FETCH trae las tres entidades en una sola consulta. Sin esto,
     * Hibernate ejecutaria una consulta adicional por cada reserva al mostrar el
     * nombre del cliente y el numero de la mesa en el listado del personal
     * (problema conocido como N+1 consultas).
     */
    @Query("""
           SELECT r FROM Reserva r
           JOIN FETCH r.cliente c
           JOIN FETCH r.mesa m
           WHERE r.fecha = :fecha
           ORDER BY r.hora ASC, m.numero ASC
           """)
    List<Reserva> buscarDelDiaConDetalle(@Param("fecha") LocalDate fecha);

    /** Total de reservas de un dia en un estado dado. Se usa en el panel. */
    long countByFechaAndEstado(LocalDate fecha, EstadoReserva estado);

    /**
     * Suma de comensales esperados en un dia.
     *
     * El calculo lo hace la base de datos y no la aplicacion: asi no se cargan
     * en memoria reservas que solo se usarian para sumar un numero.
     * COALESCE devuelve 0 cuando el dia no tiene reservas, porque SUM sobre un
     * conjunto vacio devolveria nulo.
     */
    @Query("""
           SELECT COALESCE(SUM(r.cantidadPersonas), 0) FROM Reserva r
           WHERE r.fecha = :fecha AND r.estado = :estado
           """)
    int sumarComensales(@Param("fecha") LocalDate fecha, @Param("estado") EstadoReserva estado);

    /** Historial de reservas de un cliente, de la mas reciente a la mas antigua. */
    List<Reserva> findByClienteEmailIgnoreCaseOrderByFechaDescHoraDesc(String email);
}
