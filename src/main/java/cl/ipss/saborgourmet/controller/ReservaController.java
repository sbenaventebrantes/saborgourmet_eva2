package cl.ipss.saborgourmet.controller;

import cl.ipss.saborgourmet.dto.ReservaForm;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador del sitio publico: todo lo que hace el cliente del restaurante.
 *
 * Cubre las tres operaciones que pide el caso: consultar disponibilidad y
 * reservar, modificar una reserva existente y cancelarla.
 *
 * @RequestMapping("/reservas") define el prefijo comun de todas las rutas de
 * esta clase, de modo que cada metodo solo declara la parte final de su URL.
 */
@Controller
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final MesaService mesaService;

    public ReservaController(ReservaService reservaService, MesaService mesaService) {
        this.reservaService = reservaService;
        this.mesaService = mesaService;
    }

    /**
     * Los metodos anotados con @ModelAttribute se ejecutan ANTES que cualquier
     * metodo de este controlador y agregan datos comunes al modelo. Asi los
     * turnos y los limites de fecha quedan disponibles en todas las vistas de
     * reservas sin repetir la misma linea en cada metodo.
     */
    @ModelAttribute("horarios")
    public List<LocalTime> horarios() {
        return reservaService.getHorarios();
    }

    @ModelAttribute("hoy")
    public LocalDate hoy() {
        return LocalDate.now();
    }

    @ModelAttribute("fechaMaxima")
    public LocalDate fechaMaxima() {
        return reservaService.getFechaMaxima();
    }

    // ------------------------------------------------------------------
    // 1. Consultar disponibilidad
    // ------------------------------------------------------------------

    /**
     * Muestra las mesas libres para la fecha, el turno y el numero de personas
     * que indico el cliente en el buscador.
     *
     * @RequestParam toma los valores de la URL
     * (/reservas/disponibilidad?fecha=2026-11-23&hora=20:00&personas=4) y
     * @DateTimeFormat le dice a Spring como convertir esos textos en LocalDate
     * y LocalTime.
     */
    @GetMapping("/disponibilidad")
    public String disponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime hora,
            @RequestParam int personas,
            Model model) {

        List<Mesa> disponibles = reservaService.buscarMesasDisponibles(fecha, hora, personas);

        model.addAttribute("mesas", disponibles);
        model.addAttribute("fecha", fecha);
        model.addAttribute("hora", hora);
        model.addAttribute("personas", personas);
        return "reservas/disponibilidad";
    }

    // ------------------------------------------------------------------
    // 2. Crear la reserva
    // ------------------------------------------------------------------

    /**
     * Muestra el formulario con los datos de contacto, ya asociado a la mesa que
     * el cliente eligio en la pantalla anterior.
     *
     * El objeto ReservaForm se agrega al modelo vacio pero con la mesa, la fecha
     * y la hora ya cargadas; Thymeleaf lo enlaza al formulario con th:object.
     */
    @GetMapping("/nueva")
    public String formularioNueva(
            @RequestParam Long mesaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime hora,
            @RequestParam int personas,
            Model model) {

        Mesa mesa = mesaService.obtener(mesaId);

        ReservaForm form = new ReservaForm();
        form.setMesaId(mesaId);
        form.setFecha(fecha);
        form.setHora(hora);
        form.setPersonas(personas);

        model.addAttribute("reservaForm", form);
        model.addAttribute("mesa", mesa);
        return "reservas/formulario";
    }

    /**
     * Procesa el envio del formulario y crea la reserva.
     *
     * @Valid dispara las validaciones declaradas en ReservaForm. Si alguna falla,
     * los mensajes quedan en BindingResult y se vuelve a mostrar el formulario
     * con los errores, sin perder lo que el cliente ya habia escrito.
     *
     * Cuando la reserva se crea correctamente se responde con una redireccion
     * (patron Post-Redirect-Get). Asi, si el cliente recarga la pagina, no se
     * vuelve a enviar el formulario ni se duplica la reserva.
     */
    @PostMapping
    public String crear(@Valid @ModelAttribute("reservaForm") ReservaForm form,
                        BindingResult resultado,
                        Model model,
                        RedirectAttributes flash) {

        if (resultado.hasErrors()) {
            model.addAttribute("mesa", mesaService.obtener(form.getMesaId()));
            return "reservas/formulario";
        }

        try {
            Reserva reserva = reservaService.crear(form);
            // Los flash attributes sobreviven a la redireccion y se muestran una
            // sola vez en la pagina de destino.
            flash.addFlashAttribute("exito",
                    "Su reserva fue confirmada. Guarde el codigo " + reserva.getCodigo() + ".");
            return "redirect:/reservas/" + reserva.getCodigo() + "?email=" + reserva.getCliente().getEmail();

        } catch (ReglaNegocioException ex) {
            // Si la mesa se ocupo mientras el cliente llenaba el formulario, se
            // muestra el aviso sobre el mismo formulario para que pueda corregir.
            model.addAttribute("mesa", mesaService.obtener(form.getMesaId()));
            model.addAttribute("errorNegocio", ex.getMessage());
            return "reservas/formulario";
        }
    }

    // ------------------------------------------------------------------
    // 3. Consultar, modificar y cancelar una reserva existente
    // ------------------------------------------------------------------

    /** Pantalla donde el cliente ingresa su codigo y su correo. */
    @GetMapping("/buscar")
    public String formularioBuscar() {
        return "reservas/buscar";
    }

    /** Procesa la busqueda y lleva al detalle de la reserva encontrada. */
    @PostMapping("/buscar")
    public String buscar(@RequestParam String codigo, @RequestParam String email) {
        Reserva reserva = reservaService.buscarParaCliente(codigo, email);
        return "redirect:/reservas/" + reserva.getCodigo() + "?email=" + reserva.getCliente().getEmail();
    }

    /**
     * Detalle de una reserva.
     *
     * @PathVariable toma el codigo desde la propia URL (/reservas/SG-7KD29A).
     * Se exige ademas el correo para que nadie pueda ver una reserva ajena
     * probando codigos al azar.
     */
    @GetMapping("/{codigo}")
    public String detalle(@PathVariable String codigo, @RequestParam String email, Model model) {
        model.addAttribute("reserva", reservaService.buscarParaCliente(codigo, email));
        model.addAttribute("email", email);
        return "reservas/detalle";
    }

    /** Formulario para modificar la reserva, con las mesas que quedan libres. */
    @GetMapping("/{codigo}/editar")
    public String formularioEditar(@PathVariable String codigo,
                                   @RequestParam String email,
                                   Model model) {

        Reserva reserva = reservaService.buscarParaCliente(codigo, email);

        // Se rellena el formulario con los valores actuales de la reserva.
        ReservaForm form = new ReservaForm();
        form.setMesaId(reserva.getMesa().getId());
        form.setFecha(reserva.getFecha());
        form.setHora(reserva.getHora());
        form.setPersonas(reserva.getCantidadPersonas());
        form.setNombre(reserva.getCliente().getNombre());
        form.setEmail(reserva.getCliente().getEmail());
        form.setTelefono(reserva.getCliente().getTelefono());
        form.setObservaciones(reserva.getObservaciones());

        model.addAttribute("reservaForm", form);
        model.addAttribute("reserva", reserva);
        model.addAttribute("email", email);
        model.addAttribute("mesas", mesasElegibles(reserva));
        return "reservas/editar";
    }

    /** Guarda los cambios de la reserva. */
    @PostMapping("/{codigo}/editar")
    public String editar(@PathVariable String codigo,
                         @RequestParam String email,
                         @Valid @ModelAttribute("reservaForm") ReservaForm form,
                         BindingResult resultado,
                         Model model,
                         RedirectAttributes flash) {

        Reserva reserva = reservaService.buscarParaCliente(codigo, email);

        if (resultado.hasErrors()) {
            model.addAttribute("reserva", reserva);
            model.addAttribute("email", email);
            model.addAttribute("mesas", mesasElegibles(reserva));
            return "reservas/editar";
        }

        try {
            reservaService.actualizar(codigo, form);
            flash.addFlashAttribute("exito", "Su reserva fue actualizada correctamente.");
            return "redirect:/reservas/" + codigo + "?email=" + email;

        } catch (ReglaNegocioException ex) {
            model.addAttribute("reserva", reserva);
            model.addAttribute("email", email);
            model.addAttribute("mesas", mesasElegibles(reserva));
            model.addAttribute("errorNegocio", ex.getMessage());
            return "reservas/editar";
        }
    }

    /**
     * Cancela la reserva.
     *
     * Se expone como POST y no como GET porque modifica datos: un enlace GET
     * podria dispararse por accidente al ser visitado o precargado.
     */
    @PostMapping("/{codigo}/cancelar")
    public String cancelar(@PathVariable String codigo,
                           @RequestParam String email,
                           RedirectAttributes flash) {

        reservaService.buscarParaCliente(codigo, email);
        reservaService.cancelar(codigo);
        flash.addFlashAttribute("exito", "Su reserva fue cancelada.");
        return "redirect:/reservas/" + codigo + "?email=" + email;
    }

    /**
     * Mesas que el cliente puede elegir al modificar: las que estan libres en el
     * turno actual de la reserva, mas la que ya tiene asignada (para que aparezca
     * seleccionada aunque figure como ocupada por su propia reserva).
     */
    private List<Mesa> mesasElegibles(Reserva reserva) {
        List<Mesa> disponibles = new ArrayList<>(reservaService.buscarMesasDisponibles(
                reserva.getFecha(), reserva.getHora(), reserva.getCantidadPersonas()));

        boolean yaIncluida = disponibles.stream()
                .anyMatch(m -> m.getId().equals(reserva.getMesa().getId()));
        if (!yaIncluida) {
            disponibles.add(0, reserva.getMesa());
        }
        return disponibles;
    }
}
