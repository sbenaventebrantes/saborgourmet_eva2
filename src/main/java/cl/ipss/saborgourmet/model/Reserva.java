package cl.ipss.saborgourmet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Reserva de una mesa para una fecha y un horario determinados.
 *
 * Es la entidad central del sistema y relaciona las otras dos: cada reserva
 * pertenece a un Cliente y ocupa una Mesa. El restaurante trabaja con turnos
 * fijos (ver la propiedad restaurante.horarios en application.yml), por lo que
 * basta con guardar la hora de inicio del turno.
 */
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Codigo publico de la reserva, por ejemplo "SG-7KD29A".
     * El cliente lo usa junto a su correo para consultar, modificar o cancelar
     * su reserva, de manera que no necesita crear una cuenta en el sistema.
     */
    @Column(nullable = false, unique = true, length = 12)
    private String codigo;

    /**
     * Relacion muchos a uno: muchas reservas pueden corresponder a un cliente.
     * optional = false obliga a que toda reserva tenga cliente, y @JoinColumn
     * define la columna cliente_id que actua como clave foranea.
     *
     * Se usa FetchType.EAGER (el valor por defecto de @ManyToOne) porque las
     * vistas siempre muestran el nombre del cliente junto a la reserva.
     */
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** Mesa asignada. Misma relacion muchos a uno que con el cliente. */
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "mesa_id", nullable = false)
    private Mesa mesa;

    /** Dia de la reserva. */
    @Column(nullable = false)
    private LocalDate fecha;

    /** Hora de inicio del turno reservado. */
    @Column(nullable = false)
    private LocalTime hora;

    /** Numero de comensales. Nunca puede superar la capacidad de la mesa. */
    @Column(nullable = false)
    private Integer cantidadPersonas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.CONFIRMADA;

    /** Peticiones del cliente: silla para nino, alergias, celebracion, etc. */
    @Column(length = 300)
    private String observaciones;

    /** Momento en que se registro la reserva. Sirve de historial para el local. */
    @Column(nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    /**
     * Control de concurrencia optimista. Hibernate incrementa este numero en
     * cada actualizacion y verifica que nadie mas haya modificado la fila.
     * Si dos personas editan la misma reserva a la vez, la segunda recibe un
     * error en lugar de sobrescribir silenciosamente el cambio de la primera.
     */
    @Version
    private Long version;

    /** Constructor sin argumentos exigido por JPA. */
    public Reserva() {
    }

    public Reserva(String codigo, Cliente cliente, Mesa mesa, LocalDate fecha,
                   LocalTime hora, Integer cantidadPersonas, String observaciones) {
        this.codigo = codigo;
        this.cliente = cliente;
        this.mesa = mesa;
        this.fecha = fecha;
        this.hora = hora;
        this.cantidadPersonas = cantidadPersonas;
        this.observaciones = observaciones;
        this.estado = EstadoReserva.CONFIRMADA;
        this.creadaEn = LocalDateTime.now();
    }

    /** Indica si la reserva sigue vigente, es decir, si no fue cancelada. */
    public boolean estaActiva() {
        return estado == EstadoReserva.CONFIRMADA;
    }

    /** Cambia el estado a CANCELADA, liberando la mesa en ese turno. */
    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    /**
     * Indica si la reserva ya ocurrio. Las vistas lo usan para no ofrecer los
     * botones de modificar o cancelar sobre reservas pasadas.
     */
    public boolean esPasada() {
        return fecha.atTime(hora).isBefore(LocalDateTime.now());
    }

    // ---- Getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public void setMesa(Mesa mesa) {
        this.mesa = mesa;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public Integer getCantidadPersonas() {
        return cantidadPersonas;
    }

    public void setCantidadPersonas(Integer cantidadPersonas) {
        this.cantidadPersonas = cantidadPersonas;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(LocalDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
