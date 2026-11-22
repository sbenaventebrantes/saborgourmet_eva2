package cl.ipss.saborgourmet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Persona que realiza una reserva.
 *
 * El cliente no se registra con usuario y contrasena: se identifica por su correo
 * electronico. Cuando alguien reserva por segunda vez con el mismo correo, el
 * sistema reutiliza su ficha en lugar de duplicarla (ver ReservaService).
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Correo del cliente. Es unico porque actua como identificador: junto con el
     * codigo de la reserva permite que el cliente consulte o cancele su reserva
     * sin necesidad de iniciar sesion.
     */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingrese un correo valido")
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @NotBlank(message = "El telefono es obligatorio")
    @Size(max = 20, message = "El telefono no puede superar los 20 caracteres")
    @Column(nullable = false, length = 20)
    private String telefono;

    /** Constructor sin argumentos exigido por JPA. */
    public Cliente() {
    }

    public Cliente(String nombre, String email, String telefono) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
    }

    // ---- Getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
