package cl.ipss.saborgourmet.controller;

import cl.ipss.saborgourmet.service.ReservaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

/**
 * Controlador de la pagina de inicio del sitio publico.
 *
 * La anotacion @Controller (y no @RestController) indica que los metodos
 * devuelven el NOMBRE de una vista y no datos. Spring MVC toma ese nombre,
 * por ejemplo "index", y Thymeleaf busca el archivo
 * src/main/resources/templates/index.html para generar el HTML.
 */
@Controller
public class HomeController {

    private final ReservaService reservaService;

    /** Inyeccion de dependencias por constructor. */
    public HomeController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /**
     * Pantalla de inicio con el buscador de disponibilidad.
     *
     * El objeto Model es el puente entre el controlador y la vista: todo lo que
     * se agrega con model.addAttribute queda accesible en la plantilla Thymeleaf
     * mediante la sintaxis ${nombre}.
     */
    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("horarios", reservaService.getHorarios());
        model.addAttribute("hoy", LocalDate.now());
        model.addAttribute("fechaMaxima", reservaService.getFechaMaxima());
        return "index";
    }
}
