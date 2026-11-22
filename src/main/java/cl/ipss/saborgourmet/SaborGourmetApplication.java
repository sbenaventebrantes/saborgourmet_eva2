package cl.ipss.saborgourmet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal del sistema de reservas del Restaurante Sabor Gourmet.
 *
 * La anotacion @SpringBootApplication agrupa tres anotaciones:
 *  - @Configuration: permite declarar beans dentro de esta clase.
 *  - @EnableAutoConfiguration: activa la autoconfiguracion, es decir, Spring Boot
 *    revisa las librerias disponibles y configura solo lo necesario (Tomcat,
 *    Thymeleaf, la fuente de datos y Hibernate) sin archivos XML.
 *  - @ComponentScan: busca controladores, servicios y repositorios en este paquete
 *    y en todos sus subpaquetes para registrarlos en el contenedor de Spring.
 */
@SpringBootApplication
public class SaborGourmetApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaborGourmetApplication.class, args);
    }
}
