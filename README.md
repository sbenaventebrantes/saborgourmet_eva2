# Sabor Gourmet - Sistema de reservas en linea

Proyecto de la asignatura Desarrollo de Software Web II (IF205), Evaluacion Unidad 2.

Aplicacion web que permite a los clientes del Restaurante Sabor Gourmet consultar
la disponibilidad de mesas, reservar en linea, modificar y cancelar sus reservas.
Incluye ademas una interfaz de administracion para que el personal vea las
reservas del dia y configure las mesas del local.

## Integrantes

- Sofia Benavente Brantes
- Mixiu Perez Iriarte

## Tecnologias

- Java 17
- Spring Boot 3.4.5
- Spring MVC (controladores y vistas)
- Spring Data JPA con Hibernate (persistencia)
- Thymeleaf (motor de plantillas del lado del servidor)
- Bootstrap 5 (grilla responsiva) con estilo visual neomorfico propio
- Base de datos H2 en memoria (desarrollo y pruebas)
- Maven

## Como abrir el proyecto en Visual Studio Code

1. Instalar el JDK 17 o superior.
2. Instalar las extensiones "Extension Pack for Java" y "Spring Boot Extension Pack".
3. Abrir la carpeta `sabor-gourmet` con File > Open Folder. El archivo `pom.xml`
   debe quedar en la raiz de la carpeta abierta.
4. Esperar a que VS Code descargue las dependencias de Maven.

## Como ejecutar

Desde VS Code: abrir la vista "Run and Debug", elegir "Sabor Gourmet (dev)" y
presionar el boton de iniciar. Tambien se puede abrir
`SaborGourmetApplication.java` y presionar "Run" sobre el metodo `main`.

Por terminal:

    mvn spring-boot:run

La aplicacion queda disponible en http://localhost:8080

No se necesita instalar ninguna base de datos: el perfil `dev` usa H2 en memoria
y carga automaticamente ocho mesas y dos reservas de ejemplo.

## Recorrido sugerido

1. http://localhost:8080 - buscar disponibilidad por fecha, horario y personas.
2. Elegir una mesa y completar el formulario para confirmar la reserva.
3. Anotar el codigo que entrega el sistema (por ejemplo SG-7KD29A).
4. http://localhost:8080/reservas/buscar - consultar la reserva con el codigo y
   el correo, y desde ahi modificarla o cancelarla.
5. http://localhost:8080/admin - panel del personal con las reservas del dia.
6. http://localhost:8080/admin/mesas - alta, edicion y habilitacion de mesas.

Para comprobar el diseno responsivo, abrir las herramientas de desarrollo del
navegador y activar la vista de dispositivo movil.

## Pruebas automatizadas

Desde VS Code: abrir la vista "Testing" y ejecutar las clases de prueba.

Por terminal:

    mvn test

Hay dos clases de pruebas:

- `ReservaServiceTest`: reglas de negocio (disponibilidad, creacion, doble
  reserva del mismo cupo, cancelacion, modificacion y validacion de fechas).
- `ReservaWebTest`: capa web con MockMvc (respuesta de los controladores,
  validacion de formularios y generacion de las vistas Thymeleaf).

## Estructura del proyecto

    src/main/java/cl/ipss/saborgourmet
        SaborGourmetApplication.java   Clase de arranque
        model/                         Entidades JPA: Cliente, Mesa, Reserva y sus enums
        repository/                    Interfaces de Spring Data JPA
        service/                       Logica de negocio (MesaService, ReservaService)
        controller/                    Controladores de Spring MVC
        dto/                           ReservaForm, objeto del formulario
        config/                        Carga de datos de ejemplo del perfil dev
    src/main/resources
        application.yml                Configuracion comun y parametros del restaurante
        application-dev.yml            Perfil de desarrollo (H2 en memoria)
        application-test.yml           Perfil de pruebas
        application-prod.yml           Perfil de produccion (variables de entorno)
        templates/                     Vistas Thymeleaf
        static/css/estilos.css         Hoja de estilos propia
    src/test/java                      Pruebas automatizadas

## Rutas de la aplicacion

| Metodo | Ruta                              | Descripcion                          |
|--------|-----------------------------------|--------------------------------------|
| GET    | /                                 | Inicio con el buscador               |
| GET    | /reservas/disponibilidad          | Mesas libres segun la busqueda       |
| GET    | /reservas/nueva                   | Formulario de reserva                |
| POST   | /reservas                         | Crear la reserva                     |
| GET    | /reservas/buscar                  | Consultar una reserva                |
| POST   | /reservas/buscar                  | Procesar la consulta                 |
| GET    | /reservas/{codigo}                | Detalle de la reserva                |
| GET    | /reservas/{codigo}/editar         | Formulario de modificacion           |
| POST   | /reservas/{codigo}/editar         | Guardar la modificacion              |
| POST   | /reservas/{codigo}/cancelar       | Cancelar la reserva                  |
| GET    | /admin                            | Panel con las reservas del dia       |
| POST   | /admin/reservas/{codigo}/cancelar | Cancelacion hecha por el personal    |
| GET    | /admin/mesas                      | Listado de mesas                     |
| GET    | /admin/mesas/nueva                | Formulario de mesa nueva             |
| GET    | /admin/mesas/{id}/editar          | Formulario de edicion de mesa        |
| POST   | /admin/mesas                      | Guardar la mesa                      |
| POST   | /admin/mesas/{id}/estado          | Habilitar o deshabilitar una mesa    |

## Decisiones de diseno

- Las reservas no se eliminan: al cancelarlas cambian de estado a CANCELADA, con
  lo que el restaurante conserva el historial y la mesa vuelve a quedar libre.
- El restaurante trabaja con turnos fijos, configurados en `application.yml`
  (propiedad `restaurante.horarios`), de modo que cambiar el horario de atencion
  no exige modificar el codigo.
- El cliente no necesita crear una cuenta: se identifica con el codigo de la
  reserva y su correo electronico.
- La entidad Reserva usa `@Version` para control de concurrencia optimista, de
  manera que dos modificaciones simultaneas no se sobrescriban entre si.
- El panel de administracion no tiene autenticacion porque la evaluacion de esta
  unidad se centra en Spring MVC, Spring Data JPA y Thymeleaf. En un sistema
  real se protegeria con Spring Security.
