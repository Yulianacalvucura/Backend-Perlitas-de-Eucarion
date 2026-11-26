# Perlitas de Eucarión — Backend

Backend del sistema **Perlitas de Eucarión**, una aplicación orientada a la gestión de una pyme de bisutería artesanal. El proyecto administra productos, materiales, ventas, pedidos, categorías, proveedores, gastos, producción, galería, reportes y autenticación de usuarios.

El backend está desarrollado con **Java 21 y Spring Boot**, siguiendo una arquitectura de microservicios y utilizando
un **API Gateway**
---

## Integrantes

- Yuliana Calvucura
- Francisco Riveros
- Catalina Salinas

---

## Repositorios

- **Backend:** https://github.com/Yulianacalvucura/Backend-Perlitas-de-Eucarion
- **Frontend:** https://github.com/CataSalinas/Frontend_PerlitasdeEucarion

---

## Tecnologías utilizadas

- Java 21
- Spring Boot 3.5.14
- Spring Data JPA / Hibernate
- Spring Security
- JWT
- Spring Cloud Gateway MVC
- MySQL
- Maven
- Swagger / OpenAPI
- JUnit 5 y Mockito
- Postman

---

## Arquitectura

El proyecto utiliza una arquitectura de **microservicios**. Cada servicio sigue principalmente el patrón:

```text
Controller → Service → Repository → Base de datos
```

El **Gateway** centraliza el acceso a los servicios y trabaja junto al servicio de autenticación para controlar el acceso mediante JWT y roles.

### Servicios y puertos

| Servicio | Puerto | Base de datos |
|---|---:|---|
| materiales | 8081 | db_materials |
| productos | 8082 | db_products |
| ventas | 8083 | db_ventas |
| categories | 8084 | db_categories |
| pedidos | 8085 | db_orders |
| proveedores | 8086 | db_suppliers |
| reportes | 8087 | db_reportes |
| galeria | 8088 | db_galeria |
| gastos | 8089 | db_expenses |
| service_calendario | 8090 | db_calendario |
| service-auth | 8091 | db_seguridad_perlita |
| gateway | 9090 | — |

---

## Base de datos

Actualmente los microservicios están configurados para trabajar con **MySQL local**.

Las bases de datos se crean automáticamente mediante:

```properties
createDatabaseIfNotExist=true
spring.jpa.hibernate.ddl-auto=update
```

El repositorio también incluye scripts SQL de apoyo para creación, respaldo y datos de prueba.

> Para la Evaluación 3 se encuentra pendiente completar la conexión de la base de datos a un servicio en la nube.

---

## Cómo ejecutar el backend

### 1. Requisitos

- Java JDK 21
- MySQL activo (por ejemplo, mediante XAMPP)
- Maven o Maven Wrapper

### 2. Clonar el repositorio

```bash
git clone https://github.com/Yulianacalvucura/Backend-Perlitas-de-Eucarion.git
cd Backend-Perlitas-de-Eucarion
```

### 3. Ejecutar los servicios

Desde cada carpeta se puede ejecutar:

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

Ejemplo:

```bash
cd Perlita/productos
mvnw.cmd spring-boot:run
```

El Gateway debe quedar disponible en:

```text
http://localhost:9090
```

---

## Autenticación y seguridad

El proyecto cuenta con un microservicio de autenticación (`service-auth`) y utiliza **JWT**.

Actualmente se encuentran implementados los roles:

- `ADMIN`
- `VENDEDOR`

El login se realiza mediante:

```http
POST http://localhost:9090/auth/login
```

Ejemplo de cuerpo:

```json
{
  "nombreUsuario": "admin",
  "contrasena": "admin123"
}
```

Las solicitudes protegidas utilizan el token recibido en el encabezado:

```http
Authorization: Bearer <token>
```

> El rol `CLIENTE`, requerido para completar el flujo definido en la Evaluación 3, se encuentra pendiente de integración.

---

## Swagger / OpenAPI

La documentación de los microservicios puede consultarse desde el Gateway:

```text
http://localhost:9090/swagger-ui.html
```

Desde Swagger se puede seleccionar la documentación de cada servicio y revisar sus operaciones disponibles.

Los documentos OpenAPI se exponen mediante rutas del tipo:

```text
/api/v1/productos/v3/api-docs
/api/v1/ventas/v3/api-docs
/api/v1/pedidos/v3/api-docs
/api/v1/auth/v3/api-docs
```

Actualmente el versionado `/api/v1` está aplicado a la documentación OpenAPI y a parte del sistema; queda pendiente uniformar el versionado de las rutas funcionales para cumplir completamente con la Evaluación 3.

---

## Funcionalidades principales

El backend cuenta actualmente con servicios para:

- Autenticación de usuarios mediante JWT.
- Gestión de productos.
- Gestión de materiales.
- Gestión de categorías.
- Gestión de ventas.
- Gestión de pedidos.
- Gestión de proveedores.
- Gestión de gastos.
- Gestión de galería.
- Gestión de producción y calendario.
- Generación de reportes.
- Comunicación entre distintos microservicios mediante WebClient.
- Acceso centralizado mediante API Gateway.

---

## Pruebas

El proyecto contiene pruebas con **JUnit 5 y Mockito** en varios microservicios, incluyendo pruebas de servicios y controladores.

Entre los módulos con pruebas se encuentran:

- materiales
- productos
- pedidos
- categories
- proveedores
- ventas
- gastos
- galeria
- service-auth

También existen pruebas básicas de carga del contexto en `reportes`, `service_calendario` y `gateway`.

Para ejecutar las pruebas de un servicio:

Matriz de autorizaci?n. **Criterio ?nico**: la fuente de verdad es el `SecurityConfig` de cada microservicio; el gateway valida JWT y no contradice la matriz (no aplica reglas de rol por recurso, salvo autenticaci?n/login p?blico).

| Recurso | ADMIN | VENDEDOR |
|---|---|---|
| Ventas (`/ventas`), pedidos (`/pedidos`), pedido-materiales (`/pedido-materiales`) | Todo (GET, POST, PUT, PATCH, DELETE) | GET, POST, PUT |
| Producci?n (`/api/v1/produccion/**`) | Todo | GET, POST, PUT, PATCH (incluye cambio de estado) |
| Bloqueos (`/api/v1/produccion/bloqueos/**`) | Todo | Solo GET |
| Productos (`/productos`), materiales (`/materiales`), categor?as (`/categorias`), galer?a (`/galeria`) | Todo | GET, PUT |
| Proveedores (`/proveedores`), proveedor-materiales (`/proveedor-materiales`), gastos (`/gastos`), categor?as-gasto (`/categorias-gasto`), reportes (`/reportes`) | Todo | Sin acceso |
| Auth (`/auth/registrar`, gesti?n de usuarios) | Solo ADMIN | Sin acceso |
| Login (`POST /auth/login`) | P?blico | P?blico |

Reglas generales:
- `DELETE`: siempre solo `ADMIN`.
- Falta autenticaci?n/token inv?lido: `401 Unauthorized` (formato JSON del proyecto).
- Autenticado sin permiso: `403 Forbidden` (`CustomAccessDeniedHandler`).
- No existe el rol `CLIENTE`.

---

## Variables de entorno (seguridad)

Los secretos y credenciales se externalizan con variables de entorno. Los valores por defecto son **solo para desarrollo**; en cualquier entorno compartido/producci?n deben definirse expl?citamente.

| Variable | Uso | Valor por defecto (dev) |
|---|---|---|
| `JWT_SECRET` | Secreto HMAC para firmar/validar JWT (gateway y service-auth) | secreto de desarrollo incluido en config |
| `DB_USER` | Usuario de base de datos MySQL | `root` |
| `DB_PASSWORD` | Contrase?a de base de datos MySQL | vac?o |

Aplica en todos los `application.properties`/`application.yml` de los microservicios.

## Comunicaci?n entre microservicios

- Las llamadas WebClient inter-servicio propagan `X-User-Username` y `X-User-Roles` desde la petici?n entrante (`RequestContextHolder`) mediante `ExchangeFilterFunction`.
- Se distingue `404` (recurso no existe) de errores `5xx`/red: estos ?ltimos lanzan `ServicioNoDisponibleException` y responden `503 Servicio no disponible`.
- Un microservicio llamado directamente, sin pasar por gateway y sin headers de identidad, responde `401 Unauthorized`.

## Reglas de negocio

Actualmente se encuentra implementada gran parte de la estructura del backend: microservicios, persistencia MySQL, Gateway, autenticación JWT, Swagger, lógica de negocio y pruebas en distintos servicios.

Las tareas principales pendientes para completar la Evaluación 3 son:

- Integrar el frontend React con los endpoints del backend.
- Reemplazar la persistencia simulada/local del frontend por comunicación REST.
- Completar el rol `CLIENTE` y ajustar los permisos de los tres roles requeridos: `ADMIN`, `VENDEDOR` y `CLIENTE`.
- Uniformar el versionado de las APIs bajo `/api/v1`.
- Configurar la base de datos para su uso en la nube.
- Realizar pruebas de integración frontend ↔ backend.
- Completar la documentación de APIs e integración.
- Actualizar el ERS y preparar el manual de usuario.

---

## Documentación de entrega

Para la Evaluación 3 se contempla la preparación de:

- Documento ERS final.
- Manual de usuario con capturas.
- Documentación de APIs.
- Documento de APIs e Integración.
- Repositorios públicos de frontend y backend.
- Proyecto frontend comprimido.
- Proyecto backend comprimido.

---

## Estructura general

```text
Backend-Perlitas-de-Eucarion/
├── Perlita/
│   ├── categories/
│   ├── galeria/
│   ├── gastos/
│   ├── gateway/
│   ├── materiales/
│   ├── pedidos/
│   ├── productos/
│   ├── proveedores/
│   ├── reportes/
│   ├── service-auth/
│   ├── service_calendario/
│   └── ventas/
├── perlitas_mysql.sql
├── perlitas_oracle.sql
├── db_initialization.sql
├── datos_prueba_perlita.sql
└── README.md
```

---

## Proyecto académico

Proyecto desarrollado para la asignatura **DSY1104 — Desarrollo FullStack II**, correspondiente al sistema **Perlitas de Eucarión**.
