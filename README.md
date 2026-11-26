# Perlitas de Eucarión — Backend

Backend del sistema **Perlitas de Eucarión**, una aplicación orientada a la gestión de una pyme de bisutería artesanal. El proyecto administra productos, materiales, ventas, pedidos, categorías, proveedores, gastos, producción, galería, reportes y autenticación de usuarios.

El backend está desarrollado con **Java 21 y Spring Boot**, siguiendo una arquitectura de microservicios y utilizando
un **API Gateway**. La gestión interna está integrada con el frontend React mediante REST.

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
- MySQL / MariaDB (XAMPP en desarrollo local)
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

Actualmente los microservicios están configurados para trabajar con **MySQL/MariaDB local** (puerto 3306; probado con MariaDB 10.4.32 de XAMPP).

Las bases de datos se crean automáticamente mediante:

```properties
createDatabaseIfNotExist=true
spring.jpa.hibernate.ddl-auto=update
```

Los scripts SQL históricos y los respaldos se archivaron fuera del repositorio. El esquema operativo se genera desde las entidades JPA. El diagrama `Relational.png` se conserva como referencia del modelo original.

> La integración se ha probado en desarrollo local. La pauta no exige desplegar la aplicación en la nube.

Si se reutiliza una base de datos creada antes de la integración de ventas sin cuenta, ejecutar una sola vez:

```sql
ALTER TABLE db_ventas.ventas MODIFY COLUMN cliente_id BIGINT NULL;
```

En una base nueva, Hibernate crea las tablas según las entidades actuales.

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

En Windows, iniciar MySQL desde XAMPP y ejecutar desde la raíz del backend:

```powershell
powershell -ExecutionPolicy Bypass -File .\iniciar.ps1
```

El script inicia `service-auth`, `productos`, `categories`, `ventas`, `pedidos` y `gateway`. Para iniciar los doce módulos, añadir `-Todos`. La opción `-Reiniciar` reinicia procesos Java del proyecto cuando puede identificarlos; si un puerto está ocupado por otro proceso, cerrar manualmente la terminal correspondiente.

También se puede iniciar cada servicio desde su carpeta:

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Ejemplo:

```powershell
cd Perlita/productos
.\mvnw.cmd spring-boot:run
```

El Gateway debe quedar disponible en:

```text
http://localhost:9090/api/v1
```

Para ejecutar el frontend, copiar `.env.example` a `.env` en su repositorio, usar `VITE_API_URL=/api/v1`, ejecutar `npm install` y `npm run dev`. El proxy de Vite reenvía las peticiones al Gateway.

---

## Autenticación y seguridad

El proyecto cuenta con un microservicio de autenticación (`service-auth`) y utiliza **JWT**.

Actualmente se encuentran implementados los roles:

- `ADMIN`
- `VENDEDOR`

El login se realiza mediante:

```http
POST http://localhost:9090/api/v1/auth/login
```

Ejemplo de cuerpo:

```json
{
  "nombreUsuario": "admin",
  "contrasena": "admin123"
}
```

La respuesta del login es un JWT en texto plano. El token tiene una vigencia de 24 horas; al expirar se debe iniciar sesión nuevamente (no hay refresh token). Las solicitudes protegidas utilizan el token recibido en el encabezado:

```http
Authorization: Bearer <token>
```

El Gateway valida el JWT, consulta el estado y rol actual de la cuenta en `service-auth` para las peticiones protegidas (salvo `/auth/me`, que valida la cuenta directamente), y aplica las restricciones de acceso. El personal se administra mediante `/api/v1/auth/usuarios`. Las contraseñas se guardan como hashes BCrypt.

> El cliente compra sin cuenta. El Gateway asigna internamente la identidad `CLIENTE` únicamente a tres rutas públicas exactas; los endpoints administrativos siguen requiriendo JWT. Falta comprobar este flujo en ejecución y confirmar su aceptación como cumplimiento del rol cliente.

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

El Gateway expone las rutas funcionales bajo `/api/v1` y elimina ese prefijo al dirigir las peticiones a los controladores internos que lo requieren. Se mantienen rutas anteriores por compatibilidad. Swagger utiliza `/api/v1` como servidor base de los módulos actualizados. Para consultar la documentación de todos los microservicios, deben estar iniciados. Se comprobó el acceso a Swagger de Auth y el login y consulta de productos con rutas versionadas.

---

## Funcionalidades principales

El backend cuenta actualmente con servicios para:

- Autenticación de usuarios mediante JWT.
- CRUD de productos, precios anterior y actual, stock y asociación con categorías.
- Gestión de materiales.
- Gestión de categorías.
- Registro de ventas por el administrador con datos de clientes sin cuenta, cálculo de total y descuento de stock en el servidor.
- Consulta de órdenes, actualización de estados, anulación y devolución de stock.
- Gestión de usuarios del personal (alta, consulta, edición y baja lógica).
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

Para ejecutar las pruebas de un servicio, desde su carpeta:

```powershell
.\mvnw.cmd test
```

Resultados agregados de los reportes Surefire del 26-11-2026, antes de la limpieza:

| Pruebas registradas | Fallos | Errores | Omitidas |
|---:|---:|---:|---:|
| 140 | 0 | 0 | 0 |

Estos resultados corresponden a los seis módulos principales ejecutados (autenticación, productos, categorías, ventas, pedidos y Gateway), no a una ejecución completa de los doce proyectos. Se verificaron manualmente login, categorías, CRUD de productos, ventas, descuento y devolución de stock, acceso de vendedor y rechazo HTTP 403. También se registró una compra pública y se consultó su detalle desde administración.

---

## Matriz de autorización

El Gateway controla el acceso externo y los microservicios aplican sus configuraciones de Spring Security. La siguiente tabla resume el alcance de gestión interna expuesto por el Gateway.

| Recurso / acción | ADMIN | VENDEDOR |
|---|---|---|
| Productos (`/api/v1/productos`) | CRUD | Solo GET |
| Categorías (`/api/v1/categorias`) | CRUD y relaciones | Solo GET (apoyo al catálogo) |
| Ventas (`/api/v1/ventas`) y pedidos (`/api/v1/pedidos`) | Gestión | Solo GET |
| Usuarios (`/api/v1/auth/usuarios`) | Alta, consulta, edición, baja lógica | Sin acceso |
| Perfil propio (`GET /api/v1/auth/me`) | Sí | Sí |
| Otros módulos de gestión | Según operaciones disponibles | Sin acceso a través del Gateway |
| Login (`POST /api/v1/auth/login`) | Público | Público |

Reglas generales:
- La escritura administrativa no está disponible para el vendedor.
- Falta de autenticación, token inválido o sesión invalidada: `401 Unauthorized`.
- Autenticado sin permiso: `403 Forbidden`.
- Fallos de disponibilidad de autenticación: `503 Service Unavailable`.
- El cliente sin cuenta utiliza únicamente estas operaciones públicas, sin JWT:

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/v1/productos/tienda` | Catálogo de productos activos (sin datos internos de materiales). |
| GET | `/api/v1/categorias/tienda` | Categorías activas asociadas a productos. |
| POST | `/api/v1/ventas/compra` | Compra como invitado; devuelve ID, estado, total y detalle de productos. |

Las cabeceras de identidad que envíe un visitante son reemplazadas por el Gateway. Los servicios internos permanecen en loopback; el rol CLIENTE no permite modificar productos directamente a través del Gateway.

---

## Variables de entorno (seguridad)

Los valores por defecto son solo para desarrollo. En un entorno compartido deben definirse credenciales y secretos propios.

| Variable | Uso | Valor por defecto (dev) |
|---|---|---|
| `JWT_SECRET` | Secreto HMAC compartido por Gateway y service-auth | Secreto de desarrollo incluido en configuración |
| `DB_USER` | Usuario de base de datos | `root` |
| `DB_PASSWORD` | Contraseña de base de datos | Vacía |

Los microservicios internos se configuraron para escuchar en loopback. Sus cabeceras de identidad internas no deben exponerse a clientes externos; esta configuración es para desarrollo local, no para un despliegue público.

## Comunicación entre microservicios

- El Gateway recibe las solicitudes REST versionadas, valida la identidad y propaga `X-User-Username` y `X-User-Roles`.
- WebClient utiliza estas cabeceras en llamadas entre servicios.
- Ventas consulta productos para obtener los precios y validar existencias. Ajusta el stock en el servicio de productos; al anular, devuelve las unidades y evita una segunda devolución de la misma venta.
- Se distingue un recurso inexistente (`404`) de fallos de red o disponibilidad (`503`, cuando se gestionan mediante `ServicioNoDisponibleException`).
- La compensación de ajustes de stock entre servicios no equivale a una transacción distribuida completa.

## Reglas de negocio

El sistema administrativo está integrado con React y utiliza persistencia real. La tienda pública registra compras sin cuenta, con pago en efectivo al recibir y estado `PENDIENTE`. El backend calcula el total y reserva el stock mediante los ajustes existentes; el ADMIN puede confirmar el pago o anular y devolver las unidades. No se integra una pasarela de pago.

Las tareas pendientes para cerrar la Evaluación 3 son:

- Confirmar con el docente el alcance del rol cliente invitado.
- Validar la edición, baja de cuentas y rechazo de sesiones previamente emitidas; repetir las pruebas tras la limpieza.
- Completar ERS, manual de usuario y documentación de APIs e integración.

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
├── Relational.png
├── iniciar.ps1
├── INTEGRACION_LOCAL.md
└── README.md
```

---

## Proyecto académico

Proyecto desarrollado para la asignatura **DSY1104 — Desarrollo FullStack II**, correspondiente al sistema **Perlitas de Eucarión**.
