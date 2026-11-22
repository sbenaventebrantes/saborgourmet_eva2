package cl.ipss.saborgourmet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Objeto de transferencia de datos (DTO) que representa el formulario de reserva.
 *
 * Se usa una clase aparte en lugar de enviar la entidad Reserva al formulario por
 * tres motivos:
 *  1. Los datos del formulario no coinciden con la entidad: aqui llegan el nombre
 *     y el correo del cliente sueltos, no un objeto Cliente ya existente.
 *  2. Evita que un usuario malintencionado modifique campos que no deberia tocar,
 *     como el estado o el codigo de la reserva.
 *  3. Permite validar la entrada con mensajes pensados para la pantalla.
 *
 * Spring MVC crea una instancia de esta clase y rellena sus atributos con los
 * campos del formulario que tengan el mismo nombre (enlace de datos).
 */
public class ReservaForm {

    /** Mesa elegida por el cliente en la pantalla de disponibilidad. */
    @NotNull(message = "Debe seleccionar una mesa")
    private Long mesaId;

    /**
     * Fecha de la reserva.
     * @DateTimeFormat le indica a Spring como convertir el texto que envia el
     * campo <input type="date"> (formato aaaa-mm-dd) en un objeto LocalDate.
     */
    @NotNull(message = "Debe indicar la fecha")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    /** Hora del turno, en formato HH:mm. */
    @NotNull(message = "Debe indicar el horario")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime hora;

    @NotNull(message = "Debe indicar la cantidad de personas")
    @Min(value = 1, message = "Debe reservar para al menos 1 persona")
    @Max(value = 20, message = "Para grupos de mas de 20 personas contacte al restaurante")
    private Integer personas;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingrese un correo valido")
    private String email;

    @NotBlank(message = "El telefono es obligatorio")
    @Size(max = 20, message = "El telefono no puede superar los 20 caracteres")
    private String telefono;

    @Size(max = 300, message = "Las observaciones no pueden superar los 300 caracteres")
    private String observaciones;

    // ---- Getters y setters: Spring MVC los utiliza para el enlace de datos

    public Long getMesaId() {
        return mesaId;
    }

    public void setMesaId(Long mesaId) {
        this.mesaId = mesaId;
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

    public Integer getPersonas() {
        return personas;
    }

    public void setPersonas(Integer personas) {
        this.personas = personas;
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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
