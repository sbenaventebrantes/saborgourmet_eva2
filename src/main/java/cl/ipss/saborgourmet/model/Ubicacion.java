package cl.ipss.saborgourmet.model;

/**
 * Sectores del restaurante donde puede estar ubicada una mesa.
 *
 * Se modela como enum y no como texto libre para que el valor sea siempre uno de
 * los permitidos. Cada constante lleva una etiqueta legible que las vistas
 * Thymeleaf muestran al usuario, de modo que la pantalla no exponga el nombre
 * interno de la constante.
 */
public enum Ubicacion {

    SALON("Salon principal"),
    TERRAZA("Terraza"),
    VENTANA("Junto a ventanal"),
    PRIVADO("Salon privado");

    private final String etiqueta;

    Ubicacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto que se muestra en pantalla. Thymeleaf lo lee con ${mesa.ubicacion.etiqueta}. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
