package cl.ipss.saborgourmet.config;

import cl.ipss.saborgourmet.model.Cliente;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;
import cl.ipss.saborgourmet.model.Ubicacion;
import cl.ipss.saborgourmet.repository.ClienteRepository;
import cl.ipss.saborgourmet.repository.MesaRepository;
import cl.ipss.saborgourmet.repository.ReservaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Carga datos de ejemplo al iniciar la aplicacion: el salon del restaurante con
 * sus mesas y un par de reservas de muestra.
 *
 * @Profile("dev") hace que esta clase solo se active cuando el perfil en uso es
 * "dev". En produccion no se ejecuta, de modo que nunca se insertan datos
 * ficticios en la base de datos real.
 *
 * CommandLineRunner es una interfaz de Spring Boot cuyo metodo run se ejecuta
 * una vez, justo despues de que la aplicacion termina de arrancar.
 */
@Configuration
@Profile("dev")
public class DatosIniciales {

    @Bean
    CommandLineRunner cargarDatos(MesaRepository mesas,
                                  ClienteRepository clientes,
                                  ReservaRepository reservas) {
        return args -> {

            // Mesas del local: distintos tamanos y ubicaciones para que la
            // busqueda de disponibilidad tenga resultados variados.
            List<Mesa> salon = List.of(
                    new Mesa(1, 2, Ubicacion.VENTANA),
                    new Mesa(2, 2, Ubicacion.SALON),
                    new Mesa(3, 4, Ubicacion.SALON),
                    new Mesa(4, 4, Ubicacion.TERRAZA),
                    new Mesa(5, 6, Ubicacion.TERRAZA),
                    new Mesa(6, 6, Ubicacion.SALON),
                    new Mesa(7, 8, Ubicacion.PRIVADO),
                    new Mesa(8, 12, Ubicacion.PRIVADO));
            // saveAll devuelve las mesas ya guardadas, con su id asignado.
            List<Mesa> guardadas = mesas.saveAll(salon);

            // Cliente y reservas de ejemplo para el dia de hoy, de manera que el
            // panel de administracion no aparezca vacio la primera vez.
            Cliente ejemplo = clientes.save(
                    new Cliente("Carolina Fuentes", "carolina.fuentes@example.com", "+56 9 8765 4321"));

            LocalDate hoy = LocalDate.now();
            reservas.save(new Reserva("SG-DEMO01", ejemplo, guardadas.get(2), hoy,
                    LocalTime.of(20, 0), 4, "Mesa cerca de la ventana si es posible"));
            reservas.save(new Reserva("SG-DEMO02", ejemplo, guardadas.get(4), hoy,
                    LocalTime.of(21, 0), 5, "Celebracion de cumpleanos"));
        };
    }
}
