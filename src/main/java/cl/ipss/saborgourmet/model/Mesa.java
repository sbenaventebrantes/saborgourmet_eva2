package cl.ipss.saborgourmet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Mesa fisica del restaurante. Es una de las tres entidades del modelo junto a
 * Cliente y Reserva.
 *
 * @Entity indica a JPA que esta clase se corresponde con una tabla, y @Table fija
 * el nombre "mesas" para no depender del nombre de la clase. El personal del
 * restaurante administra estas mesas desde la interfaz /admin/mesas.
 */
@Entity
@Table(name = "mesas")
public class Mesa {

    /**
     * Clave primaria. GenerationType.IDENTITY delega la numeracion en la base de
     * datos, que asigna un valor correlativo en cada insercion.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Numero visible de la mesa, el que conocen los garzones.
     * unique = true evita que existan dos mesas con el mismo numero.
     */
    @NotNull(message = "Debe indicar el numero de mesa")
    @Min(value = 1, message = "El numero de mesa debe ser mayor que cero")
    @Column(unique = true, nullable = false)
    private Integer numero;

    /** Cantidad maxima de comensales. Se usa para filtrar las mesas al buscar. */
    @NotNull(message = "Debe indicar la capacidad")
    @Min(value = 1, message = "La capacidad minima es 1 persona")
    @Max(value = 20, message = "La capacidad maxima es 20 personas")
    private Integer capacidad;

    /**
     * Sector del local. EnumType.STRING guarda el texto de la constante
     * ("TERRAZA") en lugar de su posicion numerica, de modo que agregar o
     * reordenar valores del enum no corrompe los datos ya guardados.
     */
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Debe indicar la ubicacion")
    @Column(nullable = false, length = 20)
    private Ubicacion ubicacion;

    /**
     * Permite retirar una mesa del sistema sin borrarla (por ejemplo, si esta en
     * reparacion). Una mesa inactiva no aparece entre las disponibles, pero sus
     * reservas historicas se conservan.
     */
    @Column(nullable = false)
    private boolean activa = true;

    /** Constructor sin argumentos exigido por JPA para reconstruir la entidad. */
    public Mesa() {
    }

    /** Constructor de conveniencia usado al cargar los datos de ejemplo. */
    public Mesa(Integer numero, Integer capacidad, Ubicacion ubicacion) {
        this.numero = numero;
        this.capacidad = capacidad;
        this.ubicacion = ubicacion;
        this.activa = true;
    }

    /**
     * Regla de negocio simple: la mesa sirve para un grupo si esta habilitada y
     * su capacidad alcanza. Se deja en la entidad porque depende unicamente de
     * sus propios datos.
     */
    public boolean admite(int personas) {
        return activa && capacidad != null && capacidad >= personas;
    }

    /** Texto corto usado en las vistas, por ejemplo "Mesa 4 (Terraza)". */
    public String getDescripcion() {
        return "Mesa " + numero + " (" + ubicacion.getEtiqueta() + ")";
    }

    // ---- Getters y setters: Thymeleaf y Spring MVC los necesitan para leer y
    // ---- escribir las propiedades desde las vistas y los formularios.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(Ubicacion ubicacion) {
        this.ubicacion = ubicacion;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
