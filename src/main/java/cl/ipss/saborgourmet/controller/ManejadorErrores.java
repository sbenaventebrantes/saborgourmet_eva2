package cl.ipss.saborgourmet.controller;

import cl.ipss.saborgourmet.service.ReglaNegocioException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Manejador global de errores de la aplicacion.
 *
 * @ControllerAdvice aplica a todos los controladores: cuando alguno lanza una
 * excepcion de las declaradas aqui, Spring llama a este metodo en vez de mostrar
 * la pagina de error por defecto del servidor.
 *
 * Gracias a esto los controladores no necesitan repetir bloques try/catch para
 * los casos que simplemente deben informarse al usuario.
 */
@ControllerAdvice
public class ManejadorErrores {

    /**
     * Captura los incumplimientos de reglas de negocio (mesa ocupada, codigo
     * inexistente, fecha pasada) y muestra una pagina de aviso con el mensaje
     * que escribio el servicio, en lenguaje entendible para el usuario.
     */
    @ExceptionHandler(ReglaNegocioException.class)
    public String manejarReglaNegocio(ReglaNegocioException ex, Model model) {
        model.addAttribute("mensajeError", ex.getMessage());
        return "error-negocio";
    }
}
