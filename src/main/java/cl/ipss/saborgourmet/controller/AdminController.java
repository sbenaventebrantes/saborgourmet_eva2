package cl.ipss.saborgourmet.controller;

import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;
import cl.ipss.saborgourmet.model.Ubicacion;
import cl.ipss.saborgourmet.service.MesaService;
import cl.ipss.saborgourmet.service.ReglaNegocioException;
import cl.ipss.saborgourmet.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador de la interfaz de administracion que usa el personal del
 * restaurante.
 *
 * Cubre los dos requerimientos del caso para el personal: ver las reservas
 * actuales y actualizar la configuracion de las mesas.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ReservaService reservaService;
    private final MesaService mesaService;

    public AdminController(ReservaService reservaService, MesaService mesaService) {
        this.reservaService = reservaService;
        this.mesaService = mesaService;
    }

    /** Lista de ubicaciones disponible en todas las vistas de administracion. */
    @ModelAttribute("ubicaciones")
    public Ubicacion[] ubicaciones() {
        return Ubicacion.values();
    }

    // ------------------------------------------------------------------
    // Panel de reservas
    // ------------------------------------------------------------------

    /**
     * Panel principal: resumen del dia y listado de reservas.
     *
     * El parametro fecha es opcional. required = false permite entrar a /admin
     * sin indicar nada, y en ese caso se muestra el dia de hoy.
     */
    @GetMapping
    public String panel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {

        LocalDate dia = (fecha != null) ? fecha : LocalDate.now();
        List<Reserva> reservas = reservaService.reservasDelDia(dia);

        model.addAttribute("fecha", dia);
        model.addAttribute("reservas", reservas);
        model.addAttribute("totalConfirmadas", reservaService.contarConfirmadas(dia));
        model.addAttribute("totalComensales", reservaService.contarComensales(dia));
        model.addAttribute("mesasActivas", mesaService.contarActivas());
        return "admin/panel";
    }

    /**
     * Cancelacion de una reserva hecha por el personal, por ejemplo cuando el
     * cliente avisa por telefono.
     */
    @PostMapping("/reservas/{codigo}/cancelar")
    public String cancelarReserva(@PathVariable String codigo,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                  RedirectAttributes flash) {
        try {
            reservaService.cancelar(codigo);
            flash.addFlashAttribute("exito", "Se cancelo la reserva " + codigo + ".");
        } catch (ReglaNegocioException ex) {
            flash.addFlashAttribute("errorNegocio", ex.getMessage());
        }
        return "redirect:/admin?fecha=" + fecha;
    }

    // ------------------------------------------------------------------
    // Configuracion de mesas
    // ------------------------------------------------------------------

    /** Listado completo de mesas del local. */
    @GetMapping("/mesas")
    public String listarMesas(Model model) {
        model.addAttribute("mesas", mesaService.listarTodas());
        return "admin/mesas";
    }

    /** Formulario para registrar una mesa nueva. */
    @GetMapping("/mesas/nueva")
    public String formularioNuevaMesa(Model model) {
        model.addAttribute("mesa", new Mesa());
        return "admin/mesa-formulario";
    }

    /** Formulario para editar una mesa existente. */
    @GetMapping("/mesas/{id}/editar")
    public String formularioEditarMesa(@PathVariable Long id, Model model) {
        model.addAttribute("mesa", mesaService.obtener(id));
        return "admin/mesa-formulario";
    }

    /**
     * Guarda la mesa, tanto si es nueva como si se esta editando.
     *
     * Se usa un unico metodo para ambos casos porque la entidad ya trae el id
     * cuando se trata de una edicion (va oculto en el formulario) y el metodo
     * save del repositorio inserta o actualiza segun corresponda.
     */
    @PostMapping("/mesas")
    public String guardarMesa(@Valid @ModelAttribute("mesa") Mesa mesa,
                              BindingResult resultado,
                              Model model,
                              RedirectAttributes flash) {

        if (resultado.hasErrors()) {
            return "admin/mesa-formulario";
        }

        try {
            mesaService.guardar(mesa);
            flash.addFlashAttribute("exito", "La mesa " + mesa.getNumero() + " se guardo correctamente.");
            return "redirect:/admin/mesas";

        } catch (ReglaNegocioException ex) {
            model.addAttribute("errorNegocio", ex.getMessage());
            return "admin/mesa-formulario";
        }
    }

    /** Habilita o deshabilita una mesa sin borrarla del sistema. */
    @PostMapping("/mesas/{id}/estado")
    public String cambiarEstadoMesa(@PathVariable Long id, RedirectAttributes flash) {
        Mesa mesa = mesaService.cambiarDisponibilidad(id);
        String estado = mesa.isActiva() ? "habilitada" : "deshabilitada";
        flash.addFlashAttribute("exito", "La mesa " + mesa.getNumero() + " quedo " + estado + ".");
        return "redirect:/admin/mesas";
    }
}
