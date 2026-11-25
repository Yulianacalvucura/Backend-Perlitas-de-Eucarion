# Perlitas de Eucarion

"Perlitas de Eucarion" es una pyme de bisutería que solucionó sus problemas de gestión manual con una app propia. El sistema centraliza inventario, ventas, encargos y reportes. Así, simplifica tareas, reduce errores y brinda a la dueña una visión organizada para controlar y proyectar mejor su negocio.

---

## Integrantes

- Yuliana Calvucura
- Luis Grossolli
- Catalina Salinas

---

## Aplicaciones requeridas

- Java JDK 21
- Maven
- Spring Boot 3.5.14
- MySQL (XAMPP)
- VS Code
- Postman

---

## Microservicios

| Microservicio | Puerto | Base de datos |
|---|---|---|
| service-auth | 8091 | db_seguridad_perlita |
| materiales | 8081 | db_materials |
| productos | 8082 | db_products |
| ventas | 8083 | db_ventas |
| pedidos | 8085 | db_orders |
| categories | 8084 | db_categories |
| proveedores | 8086 | db_suppliers |
| reportes | 8087 | db_reportes |
| galeria | 8088 | db_galeria |
| gastos | 8089 | db_gastos |
| service_calendario | 8090 | db_calendario |
| gateway | 9090 | - |

---

## Cómo ejecutar el proyecto

### 1. Requisitos previos
- Tener XAMPP corriendo con MySQL activo
- Tener Java 21 instalado

### 2. Clonar el repositorio
```bash
git clone https://github.com/lugrossolli-ux/perlitas_de_eucarion.git
```

### 3. Ejecutar cada microservicio (orden recomendado)
```bash
cd Perlita/service-auth && ./mvnw spring-boot:run
cd Perlita/api/v1/materiales && ./mvnw spring-boot:run
cd Perlita/api/v1/productos && ./mvnw spring-boot:run
cd Perlita/api/v1/ventas && ./mvnw spring-boot:run
cd Perlita/api/v1/pedidos && ./mvnw spring-boot:run
cd Perlita/categories && ./mvnw spring-boot:run
cd Perlita/api/v1/proveedores && ./mvnw spring-boot:run
cd Perlita/api/v1/reportes && ./mvnw spring-boot:run
cd Perlita/api/v1/galeria && ./mvnw spring-boot:run
cd Perlita/api/v1/gastos && ./mvnw spring-boot:run
cd Perlita/service_calendario && ./mvnw spring-boot:run
cd Perlita/gateway && ./mvnw spring-boot:run
```

Las bases de datos se crean automáticamente al iniciar cada microservicio.

### 4. Autenticación

Antes de consumir los endpoints, obtenga un token JWT con `POST /api/v1/auth/login`. El registro de usuarios (`POST /api/v1/auth/registrar`) requiere un token de ADMIN y solo acepta los roles `ADMIN` y `VENDEDOR`.

```bash
# Registrar usuario (requiere Authorization: Bearer <token de ADMIN>)
POST http://localhost:9090/api/v1/auth/registrar
{
  "nombreUsuario": "admin",
  "contrasena": "admin123",
  "correo": "admin@perlitas.cl",
  "roles": ["ADMIN"]
}

# Iniciar sesión
POST http://localhost:9090/api/v1/auth/login
{
  "nombreUsuario": "admin",
  "contrasena": "admin123"
}
```

Use el token recibido en el header `Authorization: Bearer <token>` para todas las peticiones.

---

## Swagger UI

El Gateway centraliza la documentación de todos los microservicios en **una sola interfaz** con un menú desplegable ("Select a definition") para cambiar entre servicios:

**http://localhost:9090/swagger-ui.html**

Internamente, cada microservicio expone su JSON de OpenAPI en una ruta propia, que el Gateway agrega:

| Microservicio | Ruta del JSON (api-docs) |
|---|---|
| service-auth | /api/v1/auth/v3/api-docs |
| materiales | /api/v1/materiales/v3/api-docs |
| productos | /api/v1/productos/v3/api-docs |
| ventas | /api/v1/ventas/v3/api-docs |
| categories | /api/v1/categorias/v3/api-docs |
| pedidos | /api/v1/pedidos/v3/api-docs |
| proveedores | /api/v1/proveedores/v3/api-docs |
| reportes | /api/v1/reportes/v3/api-docs |
| galeria | /api/v1/galeria/v3/api-docs |
| gastos | /api/v1/gastos/v3/api-docs |
| service_calendario | /api/v1/produccion/v3/api-docs |

---

## Endpoints disponibles

Todos los endpoints pueden consumirse directamente por su puerto o a través del Gateway en `http://localhost:9090`.

### Autenticación `http://localhost:9090/api/v1/auth`

| Método | Endpoint | Descripción |
|---|---|---|
| POST | /api/v1/auth/registrar | Registra un nuevo usuario |
| POST | /api/v1/auth/login | Inicia sesión y devuelve un token JWT |

### Materiales `http://localhost:9090/api/v1/materiales`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/materiales | Lista todos los materiales |
| GET | /api/v1/materiales/{id} | Busca material por ID |
| GET | /api/v1/materiales/stock-bajo | Lista materiales con stock bajo |
| POST | /api/v1/materiales | Crea un material |
| PUT | /api/v1/materiales/{id} | Actualiza un material |
| DELETE | /api/v1/materiales/{id} | Elimina un material |

### Productos `http://localhost:9090/api/v1/productos`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/productos | Lista todos los productos |
| GET | /api/v1/productos/activos | Lista productos activos |
| GET | /api/v1/productos/{id} | Busca producto por ID |
| GET | /api/v1/productos/{id}/api/v1/materiales | Ver materiales del producto |
| POST | /api/v1/productos | Crea un producto |
| POST | /api/v1/productos/{id}/api/v1/materiales | Agrega material al producto |
| PUT | /api/v1/productos/{id} | Actualiza un producto |
| DELETE | /api/v1/productos/{id} | Elimina un producto |

### Ventas `http://localhost:9090/api/v1/ventas`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/ventas | Lista todas las ventas |
| GET | /api/v1/ventas/{id} | Busca venta por ID |
| GET | /api/v1/ventas/cliente/{clienteId} | Ventas por cliente |
| GET | /api/v1/ventas/resumen/metodo-pago | Resumen por método de pago |
| POST | /api/v1/ventas | Crea una venta |

### Pedidos `http://localhost:9090/api/v1/pedidos`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/pedidos | Lista todos los pedidos |
| GET | /api/v1/pedidos/{id} | Busca pedido por ID |
| GET | /api/v1/pedidos/estado/{estado} | Pedidos por estado |
| GET | /api/v1/pedidos/cliente/{clienteId} | Pedidos por cliente |
| POST | /api/v1/pedidos | Crea un pedido |
| PUT | /api/v1/pedidos/{id} | Actualiza un pedido |
| DELETE | /api/v1/pedidos/{id} | Elimina un pedido |
| PATCH | /api/v1/pedidos/{id}/estado?nuevoEstado=... | Cambia estado del pedido |

### Pedido-Materiales `http://localhost:9090/api/v1/pedido-materiales`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/pedido-materiales/pedido/{pedidoId} | Materiales reservados de un pedido |
| POST | /api/v1/pedido-materiales | Reserva un material para un pedido |
| DELETE | /api/v1/pedido-materiales/{id} | Elimina una reserva |

### Categorías `http://localhost:9090/api/v1/categorias`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/categorias | Lista todas las categorías |
| GET | /api/v1/categorias/activas | Lista categorías activas |
| GET | /api/v1/categorias/{id} | Busca categoría por ID |
| GET | /api/v1/categorias/obtenerProducto/{id} | Productos de una categoría |
| POST | /api/v1/categorias | Crea una categoría |
| PUT | /api/v1/categorias/{id} | Actualiza una categoría |
| DELETE | /api/v1/categorias/{id} | Elimina una categoría |
| PATCH | /api/v1/categorias/reasignar | Reasigna productos entre categorías |

### Proveedores `http://localhost:9090/api/v1/proveedores`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/proveedores | Lista todos los proveedores |
| GET | /api/v1/proveedores/activos | Lista proveedores activos |
| GET | /api/v1/proveedores/{id} | Busca proveedor por ID |
| POST | /api/v1/proveedores | Crea un proveedor |
| PUT | /api/v1/proveedores/{id} | Actualiza un proveedor |
| DELETE | /api/v1/proveedores/{id} | Elimina un proveedor |
| PATCH | /api/v1/proveedores/{id}/desactivar | Desactiva un proveedor |
| PATCH | /api/v1/proveedores/{id}/activar | Activa un proveedor |

### Proveedor-Materiales `http://localhost:9090/api/v1/proveedor-materiales`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/proveedor-materiales/proveedor/{proveedorId} | Materiales de un proveedor |
| GET | /api/v1/proveedor-materiales/material/{materialId} | Proveedores de un material |
| POST | /api/v1/proveedor-materiales | Crea relación proveedor-material |
| PUT | /api/v1/proveedor-materiales/{id} | Actualiza relación |
| DELETE | /api/v1/proveedor-materiales/{id} | Elimina relación |

### Reportes `http://localhost:9090/api/v1/reportes`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/reportes | Lista todos los reportes generados |
| GET | /api/v1/reportes/{id} | Busca reporte por ID |
| POST | /api/v1/reportes | Crea un reporte manualmente |
| DELETE | /api/v1/reportes/{id} | Elimina un reporte |
| GET | /api/v1/reportes/generar/api/v1/ventas?desde=...&hasta=... | Genera reporte de ventas por período |
| GET | /api/v1/reportes/generar/stock-critico | Genera reporte de stock crítico |
| GET | /api/v1/reportes/generar/api/v1/pedidos | Genera reporte de estado de pedidos |

### Galería `http://localhost:9090/api/v1/galeria`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/galeria | Lista todas las imágenes |
| GET | /api/v1/galeria/{id} | Busca imagen por ID |
| GET | /api/v1/galeria/producto/{productoId} | Imágenes de un producto |
| POST | /api/v1/galeria | Sube una imagen |
| DELETE | /api/v1/galeria/{id} | Elimina una imagen |

### Gastos `http://localhost:9090/api/v1/gastos`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/gastos | Lista todos los gastos |
| GET | /api/v1/gastos/{id} | Busca gasto por ID |
| GET | /api/v1/gastos/categoria/{categoriaId} | Gastos por categoría |
| GET | /api/v1/gastos/proveedor/{proveedorId} | Gastos por proveedor |
| POST | /api/v1/gastos | Crea un gasto |
| PUT | /api/v1/gastos/{id} | Actualiza un gasto |
| DELETE | /api/v1/gastos/{id} | Elimina un gasto |

### Categorías de gasto `http://localhost:9090/api/v1/categorias-gasto`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/categorias-gasto | Lista categorías de gasto |
| GET | /api/v1/categorias-gasto/{id} | Busca categoría de gasto por ID |
| POST | /api/v1/categorias-gasto | Crea categoría de gasto |
| PUT | /api/v1/categorias-gasto/{id} | Actualiza categoría de gasto |
| DELETE | /api/v1/categorias-gasto/{id} | Elimina categoría de gasto |

### Calendario / Producción `http://localhost:9090/api/v1/produccion`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | /api/v1/produccion | Lista toda la producción |
| GET | /api/v1/produccion/{id} | Busca producción por ID |
| GET | /api/v1/produccion/calendario?desde=...&hasta=... | Producción en rango de fechas |
| POST | /api/v1/produccion | Crea una orden de producción |
| PUT | /api/v1/produccion/{id} | Actualiza producción |
| PATCH | /api/v1/produccion/{id}/estado?nuevoEstado=... | Cambia estado de producción |
| DELETE | /api/v1/produccion/{id} | Elimina producción |
| GET | /api/v1/produccion/bloqueos | Lista bloqueos |
| POST | /api/v1/produccion/bloqueos | Crea un bloqueo |
| DELETE | /api/v1/produccion/bloqueos/{id} | Elimina un bloqueo |

---

## Datos de prueba

### Registrar un usuario
```json
POST http://localhost:9090/api/v1/auth/registrar
{
  "nombreUsuario": "vendedor1",
  "contrasena": "vendedor123",
  "correo": "vendedor@perlitas.cl",
  "roles": ["VENDEDOR"]
}
```

### Crear un material
```json
POST http://localhost:9090/api/v1/materiales
{
  "nombre": "Hilo de seda",
  "descripcion": "Hilo fino para pulseras",
  "unidadMedida": "metros",
  "stockActual": 100,
  "stockMinimo": 10,
  "precioUnitario": 500
}
```

### Crear un producto
```json
POST http://localhost:9090/api/v1/productos
{
  "nombre": "Pulsera trenzada",
  "descripcion": "Pulsera hecha a mano con hilo de seda",
  "precioVenta": 5000,
  "stockDisponible": 20,
  "activo": true
}
```

### Crear una venta
```json
POST http://localhost:9090/api/v1/ventas
{
  "clienteId": 1,
  "metodoPago": "efectivo",
  "productos": [
    {
      "productoId": 1,
      "cantidad": 2,
      "precioUnitario": 5000
    }
  ]
}
```

### Crear un pedido
```json
POST http://localhost:9090/api/v1/pedidos
{
  "clienteId": 1,
  "descripcion": "Pulsera personalizada con nombre",
  "estado": "pendiente",
  "fechaEntrega": "2026-06-01",
  "total": 5000,
  "abonoPagado": 0
}
```

### Cambiar estado de un pedido
```
PATCH http://localhost:9090/api/v1/pedidos/1/estado?nuevoEstado=en_fabricacion
```

Estados válidos: `pendiente` → `en_fabricacion` → `listo_para_entrega` → `entregado`

### Cambiar estado de una producción
```
PATCH http://localhost:9090/api/v1/produccion/1/estado?nuevoEstado=en_proceso
```

Estados válidos: `programado` → `en_proceso` → `finalizado`. Se puede cancelar desde cualquier estado activo (`cancelado`).

### Crear una categoría
```json
POST http://localhost:9090/api/v1/categorias
{
  "nombre": "Pulseras",
  "descripcion": "Todo tipo de pulseras",
  "activo": 1
}
```

### Generar reporte de ventas
```
GET http://localhost:9090/api/v1/reportes/generar/ventas?desde=2026-01-01&hasta=2026-06-21
```

---

## Comunicación entre microservicios

| Servicio origen | Consulta a | Propósito |
|---|---|---|
| productos | materiales (8081) | Enriquecer productos con datos del material |
| ventas | productos (8082) | Enriquecer ventas con datos del producto vendido |
| pedidos | productos (8082) | Verifica que el producto exista antes de crear el pedido |
| gastos | proveedores (8086) | Enriquecer gastos con datos del proveedor |
| reportes | productos, materiales, ventas, pedidos, gastos | Consolidar datos para generar reportes ⚠️ |
| service_calendario | productos (8082), pedidos (8085) | Enriquecer producción con datos de producto y pedido ⚠️ |
| galeria | - | Servicio autónomo |
| categories | - | Servicio autónomo |

El **Gateway** (puerto 9090) centraliza el acceso a todos los microservicios, valida tokens JWT y controla acceso por roles.

---

## Mapeo de rutas (versión /api/v1)

| Ruta antigua | Ruta nueva |
|---|---|
| `/auth/**` | `/api/v1/auth/**` |
| `/materiales/**` | `/api/v1/materiales/**` |
| `/productos/**` | `/api/v1/productos/**` |
| `/ventas/**` | `/api/v1/ventas/**` |
| `/categorias/**` | `/api/v1/categorias/**` |
| `/pedidos/**` | `/api/v1/pedidos/**` |
| `/pedido-materiales/**` | `/api/v1/pedido-materiales/**` |
| `/proveedores/**` | `/api/v1/proveedores/**` |
| `/proveedor-materiales/**` | `/api/v1/proveedor-materiales/**` |
| `/reportes/**` | `/api/v1/reportes/**` |
| `/galeria/**` | `/api/v1/galeria/**` |
| `/gastos/**` | `/api/v1/gastos/**` |
| `/categorias-gasto/**` | `/api/v1/categorias-gasto/**` |

> `service_calendario` ya usaba `/api/v1/produccion/**` y se mantiene sin cambios.

---

## Roles y permisos

| Rol | Permisos |
|---|---|
| ADMIN | Acceso completo a todos los endpoints |
| VENDEDOR | GET, POST, PUT - no puede eliminar recursos |

Solo ADMIN puede registrar usuarios. No existe el rol CLIENTE.

---

## Reglas de negocio

- Los pedidos solo pueden avanzar en orden: `pendiente` → `en_fabricacion` → `listo_para_entrega` → `entregado`. No se puede retroceder ni saltar estados.
- Las producciones siguen: `programado` → `en_proceso` → `finalizado`. Desde cualquier estado activo se puede pasar a `cancelado`.
- No se puede reasignar productos a una categoría inactiva.
- El total de una venta se calcula automáticamente en base a los productos y sus cantidades.
- La fecha de un pedido se asigna automáticamente al momento de su creación.
- No se puede programar producción si existe un bloqueo en el rango de fechas.
- Cada microservicio valida los tokens JWT recibidos del gateway antes de procesar la solicitud.

---

## Tests

Se implementaron tests unitarios con JUnit 5 y Mockito para el 60% de los microservicios:

| Microservicio | Service Tests | Controller Tests |
|---|---|---|---|
| materiales | ✅ | ✅ |
| productos | ✅ | ✅ |
| pedidos | ✅ | ✅ |
| categories | ✅ | ✅ |
| proveedores | ✅ | ✅ |
| ventas | ✅ | ✅ |
| gastos | ✅ | ✅ |
| galeria | ✅ | ✅ |
| service-auth | ✅ | ❌ |

Ejecutar tests:
```bash
cd Perlita/<microservicio>
./mvnw test
```

---

## Arquitectura

El proyecto sigue una arquitectura de microservicios con:

- **Patrón CSR** (Controller-Service-Repository) en cada microservicio
- **API Gateway** como punto de entrada único
- **Autenticación JWT** centralizada en el gateway
- **Comunicación síncrona** vía WebClient entre microservicios
- **Base de datos independiente** por microservicio (Database-per-Service)
- **Documentación OpenAPI/Swagger** expuesta a través del gateway