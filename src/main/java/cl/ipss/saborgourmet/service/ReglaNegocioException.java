package cl.ipss.saborgourmet.service;

/**
 * Excepcion que se lanza cuando se incumple una regla de negocio, por ejemplo al
 * intentar reservar una mesa ya ocupada o al cancelar una reserva inexistente.
 *
 * Extiende RuntimeException para no obligar a declararla en cada metodo. El
 * controlador la captura y transforma su mensaje en una alerta en pantalla, de
 * modo que el usuario recibe una explicacion entendible en lugar de un error
 * tecnico del servidor.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
