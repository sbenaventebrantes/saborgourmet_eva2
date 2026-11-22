package cl.ipss.saborgourmet;

import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Ubicacion;
import cl.ipss.saborgourmet.repository.MesaRepository;
import cl.ipss.saborgourmet.repository.ReservaRepository;
import cl.ipss.saborgourmet.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pruebas de la capa web: comprueban que los controladores respondan y que las
 * plantillas Thymeleaf se generen sin errores.
 *
 * MockMvc simula peticiones HTTP sin levantar un servidor real, por lo que las
 * pruebas son rapidas y no ocupan el puerto 8080.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservaWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ReservaService reservaService;

    private LocalDate manana;
    private String turno;

    @BeforeEach
    void prepararDatos() {
        // Primero las reservas, que referencian a las mesas por clave foranea.
        reservaRepository.deleteAll();
        mesaRepository.deleteAll();
        mesaRepository.save(new Mesa(201, 4, Ubicacion.TERRAZA));
        manana = LocalDate.now().plusDays(1);
        turno = reservaService.getHorarios().get(0).toString();
    }

    @Test
    @DisplayName("La pagina de inicio se muestra con el buscador")
    void laPaginaDeInicioSeMuestra() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Buscar disponibilidad")));
    }

    @Test
    @DisplayName("La busqueda de disponibilidad lista las mesas libres")
    void laBusquedaListaMesasLibres() throws Exception {
        mvc.perform(get("/reservas/disponibilidad")
                        .param("fecha", manana.toString())
                        .param("hora", turno)
                        .param("personas", "4"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/disponibilidad"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mesa 201")));
    }

    @Test
    @DisplayName("El envio del formulario crea la reserva y redirige al detalle")
    void elFormularioCreaLaReserva() throws Exception {
        Long mesaId = mesaRepository.findByNumero(201).orElseThrow().getId();

        mvc.perform(post("/reservas")
                        .param("mesaId", mesaId.toString())
                        .param("fecha", manana.toString())
                        .param("hora", turno)
                        .param("personas", "4")
                        .param("nombre", "Pedro Rojas")
                        .param("email", "pedro@example.com")
                        .param("telefono", "+56 9 3333 4444"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/reservas/SG-*"));
    }

    @Test
    @DisplayName("Un formulario incompleto vuelve a la vista con los errores")
    void formularioIncompletoMuestraErrores() throws Exception {
        Long mesaId = mesaRepository.findByNumero(201).orElseThrow().getId();

        mvc.perform(post("/reservas")
                        .param("mesaId", mesaId.toString())
                        .param("fecha", manana.toString())
                        .param("hora", turno)
                        .param("personas", "4")
                        .param("nombre", "")
                        .param("email", "no-es-un-correo")
                        .param("telefono", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/formulario"))
                .andExpect(model().attributeHasFieldErrors("reservaForm", "nombre", "email", "telefono"));
    }

    @Test
    @DisplayName("El panel de administracion se muestra con el resumen del dia")
    void elPanelDeAdministracionSeMuestra() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/panel"))
                .andExpect(model().attributeExists("totalConfirmadas", "totalComensales", "mesasActivas"));
    }

    @Test
    @DisplayName("La configuracion de mesas lista las mesas del local")
    void laConfiguracionDeMesasSeMuestra() throws Exception {
        mvc.perform(get("/admin/mesas"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/mesas"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Configuracion de mesas")));
    }

    @Test
    @DisplayName("Un codigo de reserva inexistente muestra la pagina de aviso")
    void codigoInexistenteMuestraAviso() throws Exception {
        mvc.perform(get("/reservas/SG-NOEXIS").param("email", "nadie@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("error-negocio"));
    }
}
