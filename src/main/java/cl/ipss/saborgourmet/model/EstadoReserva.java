package cl.ipss.saborgourmet.model;

/**
 * Estados por los que puede pasar una reserva.
 *
 * Las reservas nunca se borran de la base de datos: al cancelarlas cambian de
 * estado a CANCELADA. Esto conserva el historial para el restaurante y permite
 * que la mesa vuelva a quedar disponible, porque el calculo de disponibilidad
 * solo considera las reservas en estado CONFIRMADA.
 */
public enum EstadoReserva {

    CONFIRMADA("Confirmada"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
