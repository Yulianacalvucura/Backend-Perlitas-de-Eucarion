# Análisis detallado — Backend Perlitas de Eucarion

**Fecha del análisis:** 2026-11-25  
**Repositorio:** `Backend-Perlitas-de-Eucarion` (rama `developer`, 3 commits, árbol limpio)  
**Alcance:** 12 proyectos Maven/Spring Boot bajo `Perlita/`, scripts SQL, README, configuración de VS Code y estado de Git.  
**Metodología:** revisión estática de código, configuración, documentación y datos semilla. No se ejecutaron los servicios ni se modificó código existente.

---

## 1. Ficha técnica del proyecto

| Aspecto | Valor |
|---|---|
| Dominio | Gestión de una pyme de bisutería: inventario, productos, ventas, pedidos, producción, proveedores, gastos, galería y reportes |
| Estilo arquitectónico | Microservicios, patrón CSR (Controller → Service → Repository), base de datos por servicio |
| Stack | Java 21, Spring Boot 3.5.14, Spring MVC + WebClient, Spring Security, JWT (jjwt 0.11.5), JPA/Hibernate, MySQL, springdoc/OpenAPI, JUnit 5 + Mockito |
| Servicios | `service-auth` (8091), `materiales` (8081), `productos` (8082), `ventas` (8083), `categories` (8084), `pedidos` (8085), `proveedores` (8086), `reportes` (8087), `galeria` (8088), `gastos` (8089), `service_calendario` (8090), `gateway` (9090) |
| Punto de entrada | Gateway MVC con 25 rutas (11 de negocio, 11 de documentación, calendario y auth) |
| Seguridad | JWT HS256 emitido por `service-auth`; gateway valida y reenvía identidad mediante cabeceras `X-User-Username` / `X-User-Roles`; cada servicio construye su `Authentication` con esas cabeceras |
| Persistencia | `ddl-auto=update`, `root` sin contraseña, `createDatabaseIfNotExist=true` en todos los servicios |
| Comunicación | WebClient síncrono (`.block()`), URLs `localhost` embebidas en código y properties, sin timeouts ni circuit breaker |
| Pruebas | 147 `@Test` en 29 archivos; 9 servicios con tests de service/controller y 3 solo con `contextLoads` |
| Documentación | README extenso con endpoints, reglas de negocio, roles y una sección de “bugs conocidos” parcialmente obsoleta |

**Inventario observado:** 12 `pom.xml`, 11 `application.properties` + 1 `application.yml`, ~180 clases Java (main + test), 5 scripts SQL, 1 PDF de informe, 1 imagen del modelo relacional.

---

## 2. Arquitectura real y flujo de petición

```
Cliente
  │  POST /auth/registrar | /auth/login  (público)
  ▼
Gateway :9090  →  service-auth :8091  →  JWT HS256 (24 h, claim "roles")
  │  AuthenticationFilter valida firma/expiración y aplica reglas por rol/método
  │  Inyecta X-User-Username y X-User-Roles
  ▼
Microservicio :8081..:8090
  │  RequestHeaderAuthenticationFilter crea Authentication desde esas cabeceras
  │  SecurityConfig aplica reglas por método HTTP
  ▼
MySQL propia por servicio  (+ WebClient a servicios vecinos)
```

**Rutas del gateway (`Perlita/gateway/src/main/resources/application.yml`):**
- Negocio: `/auth/**`, `/materiales/**`, `/productos/**`, `/ventas/**`, `/categorias/**`, `/pedidos/**`, `/pedido-materiales/**`, `/proveedores/**`, `/proveedor-materiales/**`, `/reportes/**`, `/galeria/**`, `/gastos/**`, `/categorias-gasto/**`, `/api/v1/produccion/**`.
- Documentación: `/api/v1/<servicio>/v3/api-docs` para los 11 servicios con OpenAPI.
- Swagger agregado en `http://localhost:9090/swagger-ui.html` con selector de definiciones.

**Valoración:** la separación por dominio y la uniformidad CSR son adecuadas para un proyecto académico/demo. El problema está en la frontera de confianza: el gateway valida el JWT, pero los servicios no lo validan y aceptan cabeceras planas como prueba de identidad.

---

## 3. Hallazgos críticos (P0)

| ID | Hallazgo | Evidencia | Impacto |
|---|---|---|---|
| C1 | Registro público permite autoasignarse `ADMIN` | `service-auth/.../dto/AuthRequest.java:31` expone `Set<String> roles`; `AuthService.java:40-49` los asigna directamente desde la BD; `/auth/**` es `permitAll` en `SecurityConfig.java:21`; README lo muestra como uso normal en `README.md:76-86` | Cualquier anónimo se registra con `roles:["ADMIN"]`, hace login y obtiene un JWT administrativo legítimo |
| C2 | Los microservicios confían ciegamente en `X-User-*` | `Perlita/materiales/.../config/RequestHeaderAuthenticationFilter.java:22-34` (copia idéntica en los 10 servicios de negocio); `README.md:126` invita a consumir puertos directos | `curl -H "X-User-Roles: ADMIN" http://host:8081/materiales ...` otorga privilegios sin token. `README.md:432` afirma falsamente que cada servicio valida el JWT |
| C3 | Secreto JWT hardcoded y duplicado | `gateway/src/main/resources/application.yml:5` y `service-auth/src/main/resources/application.properties:8` contienen la misma clave HS256 literal | Cualquier lector del repositorio puede firmar tokens `ADMIN`. No hay rotación ni distinción por entorno |
| C4 | Excepción en `shouldNotFilter` del gateway | `AuthenticationFilter.java:28-33`: `path.startsWith("/auth")` y `path.contains("/v3/api-docs")` son coincidencias de texto, no matchers de ruta | Una URI como `/materiales/v3/api-docs` cumple la ruta de negocio (`application.yml:21`) y está eximida del filtro: no se valida JWT, no se sobrescriben cabeceras y el servicio downstream autoriza con las cabeceras falsificadas (C2). Requiere prueba de integración para confirmar el comportamiento final |
| C5 | Máquina de estados eludible por `PUT` | `pedidos/.../service/PedidoService.java:62-84` valida transiciones solo en `cambiarEstado`, pero `PedidoController.java:56-62` expone `PUT /pedidos/{id}` que acepta un `Pedido` completo con `estado` arbitrario. Igual en `service_calendario/.../ProduccionService.java:57-70` | Se puede saltar de `pendiente` a `entregado` o alterar estados de producción sin validación; las reglas del README no son realmente enforced |
| C6 | Serialización circular Producto ↔ ProductoMaterial | `productos/.../model/Producto.java:38-39` (`@OneToMany` EAGER) y `ProductoMaterial.java:19-21` (campo `producto` sin `@JsonIgnore`); `VentaProducto.java:20-23` sí lo tiene | `GET /productos`, `GET /productos/{id}` y `POST /productos/{id}/materiales` pueden responder 500 por autorreferencia de Jackson |
| C7 | `crearVenta` realiza doble escritura no atómica | `ventas/.../VentaService.java:49-51` guarda con `cascade = ALL` (`Venta.java:44`) **antes** de asignar `p.setVenta(guardada)` y luego llama a `saveAll`; no hay `@Transactional` | Filas huérfanas en `venta_producto` si falla el segundo guardado; total calculado con precios enviados por el cliente, no consultados al catálogo |

> Nota: C1 + C2 + C3 + C4 deben corregirse de forma coordinada. Arreglar solo una deja rutas alternativas de escalada.

---

## 4. Hallazgos por área

### 4.1. Autenticación, gateway y seguridad transversal

**Funcionamiento actual**
- `JwtService.java:19-27`: HS256, `sub = username`, claim `roles`, `iat`, `exp = 24 h`. No incluye `iss`, `aud`, `jti` ni `nbf`; no hay logout, blacklist ni renovación.
- `AuthenticationFilter.java:39-81`: exige `Bearer`, parsea claims, aplica reglas gruesas por rol (`CLIENTE` solo GET; `VENDEDOR` no DELETE; `ADMIN` sin límite) y reenvía identidad通过 wrapper.
- `DataSeeder.java:23-39`: crea `admin/admin123` y `vendedor/vendedor123` (BCrypt), pero con guard `if (rolRepository.count() > 0) return;` en la línea 21: si los roles ya existen por SQL, **no crea usuarios**.
- `AuthRequest.java:26-29`: `correo` es obligatorio y válido como email también en login, pero `AuthService.login` nunca lo usa; el ejemplo de login del README lo omite → 400.
- `GlobalExceptionHandler` de auth devuelve 400 para “Credenciales inválidas” aunque Swagger documenta 401; además expone `ex.getMessage()` en respuestas 500.

**Matriz de permisos inconsistente**

| Servicio | GET | POST | PUT | PATCH | DELETE |
|---|---|---|---|---|---|
| materiales / productos | ADMIN, VENDEDOR | ADMIN | ADMIN, VENDEDOR | — | ADMIN |
| ventas | ADMIN, VENDEDOR | ADMIN, VENDEDOR | ADMIN, VENDEDOR | — | ADMIN |
| pedidos / pedido-materiales | ADMIN, VENDEDOR | ADMIN | ADMIN, VENDEDOR | **sin regla** → cualquier autenticado | ADMIN, VENDEDOR |
| categories | ADMIN, VENDEDOR | ADMIN | ADMIN | **sin regla** | ADMIN |
| proveedores | ADMIN, VENDEDOR | ADMIN | ADMIN | **sin regla** | ADMIN |
| reportes / gastos / galería | ADMIN, VENDEDOR | ADMIN | ADMIN, VENDEDOR | — | ADMIN |
| service_calendario | ADMIN, VENDEDOR | ADMIN | ADMIN, VENDEDOR | ADMIN, VENDEDOR | ADMIN |

Consecuencias:
- `CLIENTE` puede hacer GET según el gateway, pero los servicios lo rechazan con 403.
- `VENDEDOR` puede reasignar categorías, activar/desactivar proveedores y cambiar estados de pedidos vía PATCH, aunque el README diga lo contrario.
- En `service_calendario/.../SecurityConfig.java:20-27` los matchers genéricos `/api/v1/produccion/**` preceden a los específicos de `/bloqueos/**`, por lo que estos últimos son inalcanzables.
- `authenticationEntryPoint` devuelve 403 para usuario no autenticado en lugar de 401 (`materiales/.../SecurityConfig.java:35-39`).
- `CustomAccessDeniedHandler.java:21-26` siempre menciona “tu cuenta de VENDEDOR”, aunque el denegado sea `CLIENTE`.

**Otros problemas de seguridad**
- `@CrossOrigin(origins = "*")` en todos los controllers anula la intención restrictiva de los `WebConfig` (`http://localhost:9090`) y de `spring.mvc.cors.*` (además, en Boot 3 la propiedad correcta es `spring.web.cors.*`).
- `OPTIONS` de rutas protegidas exige `Bearer` en el gateway → preflight CORS roto.
- Sin TLS, sin rate limiting en `/auth/login`, sin bloqueo de cuentas y sin límite de tamaño de body.
- `jjwt 0.11.5` con API deprecada (`builder()`/`parserBuilder()`); scopes `compile` vs `runtime` inconsistentes entre auth y gateway.
- Cero tests de seguridad: los `@WebMvcTest` usan `addFilters = false`; el gateway solo tiene `contextLoads`.

### 4.2. Microservicios núcleo

#### `materiales` (8081)
- CRUD coherente con el README y fecha de actualización en `guardar`.
- `MaterialRepository.java:12`: `stockActual < stockMinimo` — **el bug #4 del README ya está corregido**.
- Sin `@Positive` en stock/precio; `Double` para importes; sin paginación.
- El test de servicio mockea `findStockBajo()`, por lo que nunca ejecuta la query JPQL real.

#### `productos` (8082)
- CRUD y relación producto-material. Enriquecimiento vía WebClient a `http://localhost:8081/materiales/{id}` **hardcodeado** en `ProductoService.java:52`, sin timeout, con `catch (Exception)` que devuelve `"Material no disponible"`.
- Patrón N+1: un `.block()` secuencial por material.
- `verMateriales` no valida existencia del producto; `agregarMaterial` no valida material ni duplicados.
- Ciclo de serialización descrito en C6 y `@Data` en entidades JPA con relaciones EAGER → riesgo de `StackOverflowError` en `toString`/`equals`.

#### `ventas` (8083)
- Cálculo de total en `VentaService.java:43-47` usando `precioUnitario` del payload: el cliente puede manipular el precio.
- No existe descuento de stock aunque Swagger lo promete en `VentaController.java:51-52`.
- `@NotNull fecha` en `Venta.java:40-42` contradice la asignación automática del servicio (`VentaService.java:41`); el test de creación no envía `fecha` y espera 201.
- Enriquecimiento N+1 a `http://localhost:8082/productos/{id}` sin timeout y con errores silenciados.
- `resumenPorMetodoPago()` devuelve `List<Map<String,Object>>` sin DTO; `Double` para dinero.

#### `pedidos` (8085)
- `PedidoService.java:62-84` implementa correctamente `pendiente → en_fabricacion → listo_para_entrega → entregado`, sin retroceso, pero sin `@Transactional`, sin `@Version` y con `ResponseEntity` en la capa de servicio.
- C5: `PUT /pedidos/{id}` evita la validación.
- `verificarProductoExiste()` (`:86-96`) **nunca se invoca**; usa `onErrorResume → false`, de modo que un timeout se interpreta como “producto inexistente”. El README afirma que sí se usa.
- `calendarioWebClient` está inyectado pero sin uso.
- `PedidoMaterialService.guardar` no verifica pedido, material, stock ni unicidad del par.
- Swagger documenta estados obsoletos (`en_proceso`, `completado`) en `PedidoController.java:77,84`.
- Datos semilla usan estados inválidos (`en_proceso`, `cancelado`) en `datos_prueba_perlita.sql:121,124`.

### 4.3. Servicios de soporte

#### `categories` (8084)
- CRUD y regla de no reasignar a categoría inactiva, pero solo compara `activo == 0` (`CategoriaService.java:59`): valores como `2` o `-1` pasan.
- No hay API para vincular productos a categorías; los vínculos solo existen en SQL semilla. Al crear productos desde la API nunca quedan clasificados.
- Borrar una categoría con productos viola la FK y termina en 500 (`DataIntegrityViolationException` no manejada).
- JPQL de bulk update en `ProductoCategoriaRepository.java:21-24` (`SET pc.categoria.id = :destino`) es frágil en Hibernate 6, no está cubierta por tests, no valida `origen == destino` ni evita duplicados, y la transacción está en el repositorio, no en el servicio.

#### `proveedores` (8086)
- CRUD y activar/desactivar correctos; `fechaActualizacion` sobrescrita en cada guardado.
- `ProveedorMaterial` usa `Long proveedorId`/`materialId` sin `@ManyToOne`, sin validación de existencia, sin `@UniqueConstraint` y sin `@DecimalMin`; el script canónico sí tiene FKs y `DECIMAL(10,2)`, pero con BDs separadas y `ddl-auto=update` no se crean.
- PUT permite cambiar ambos IDs de la relación.
- PATCH sin regla de rol → VENDEDOR puede activar/desactivar proveedores.

#### `gastos` (8089)
- **La validación de proveedor nunca funciona:** `GastoService.java:43-53` llama a `http://localhost:8086/proveedores/{id}` sin propagar identidad; `proveedores` responde 403; `onErrorResume → false` se convierte en `ResourceNotFoundException("Proveedor no encontrado")`. Todo gasto con `proveedorId` falla con 404 engañoso.
- `categoriaId` nunca se valida contra `CategoriaGastoService`.
- `@NotNull fecha` en `Gasto.java:33-35` hace inalcanzable el valor por defecto del servicio, porque `@Valid` actúa antes.
- `monto` es `Double` sin `@Positive`; `comprobanteRef` sin formato ni archivo adjunto.

#### `galeria` (8088)
- Es un catálogo de URLs, **no una galería con subida de archivos**: `GaleriaController.java:53-56` recibe un JSON con `urlImagen`; no hay `MultipartFile`, configuración `multipart`, almacenamiento ni endpoint para servir bytes. El README dice “Sube una imagen”.
- No valida `productoId`, no filtra por `activo`, no garantiza una única imagen principal y `DELETE` devuelve 204 incluso si el ID no existe.

### 4.4. Reportes y calendario

**`reportes` (8087)**
- Las rutas ya están corregidas: `/ventas`, `/productos`, `/materiales/stock-bajo`, `/pedidos` en `ReporteService.java:59,66,89,112`.
- `application.properties:23` ya apunta a `8085`, pero queda un fallback peligroso en `WebClientConfig.java:20`: `${microservicio.pedidos.url:http://localhost:8084}` (puerto de `categories`).
- `generarReporteVentas` ignora `desde`/`hasta`: solo los guarda en `parametros` (`:75`) y cuenta todas las ventas y productos (`:58-70`). No hay montos ni detalle.
- `onErrorResume(e -> Mono.empty())` en `:62,69,92,115` convierte 403/500/timeout en conteos `0`, sin logging. **Esta es la causa residual de los reportes vacíos**, no los prefijos documentados.
- `gastosWebClient` está inyectado pero nunca se usa.

**`service_calendario` (8090)**
- Las rutas a `/productos/{id}` y `/pedidos/{id}` ya están corregidas (`ProduccionService.java:127,141`).
- C5: `actualizar()` copia `estado`, fechas y notas sin validar transiciones ni solapes; `cambiarEstado()` sí valida (`:72-97`) y asigna `fechaFinReal` al finalizar.
- `validarSinSolapeConBloqueos()` solo se ejecuta al crear; crear un bloqueo nuevo no verifica producciones ya programadas; no hay `@Transactional` ni bloqueo optimista → carrera crítica.
- `obtenerCalendario` filtra solo `fechaInicio BETWEEN desde AND hasta` (`ProduccionRepository.java:18`), aunque Swagger promete solape de rangos.
- El contrato real de PATCH es un cuerpo JSON `{"estado":"..."}` (`ProduccionController.java:79-82`); el README usa `?nuevoEstado=...` y la ruta inexistente `/calendario/produccion/...` (`README.md:279,363`).
- `enriquecerProduccion` silencia errores con `Mono.empty()` → `datosProducto`/`datosPedido` quedan `null` ante cualquier fallo, incluida la autenticación.
- `Produccion.java:29-35` usa solo `@Column(nullable=false)` sin `@NotNull`, por lo que fechas faltantes generan `NullPointerException` → 500 en lugar de 400.

### 4.5. Base de datos y scripts SQL

- `perlitas_mysql.sql` y `perlitas_oracle.sql` son esquemas **monolíticos** con FKs cruzadas (productos↔materiales, ventas↔productos, pedidos↔clientes) imposibles de mantener con database-per-service y `ddl-auto=update`. Las entidades usan IDs escalares sin `@ManyToOne`, por lo que esas restricciones no se materializan.
- Faltan tablas de `roles`, `usuarios` y `usuario_roles` en ambos scripts, aunque `DataSeeder` las requiere. `db_initialization.sql:12-14` inserta roles que chocarían con la constraint `unique` de `Rol.java:33`.
- `direccion` existe en la entidad `Proveedor` y en los seeds, pero no en los scripts canónicos.
- Faltan entidades para `clientes` y para `etiquetas`/`galeria_etiqueta`.
- Nombres de BD inconsistentes entre README y scripts: `db_seguridad_perlita` vs `db_service_auth`; `db_gastos` vs `db_expenses`; `db_suppliers` vs `db_proveedores`.
- `Reporte.java:29,33` usa `columnDefinition="TEXT"` frente a `CLOB` en Oracle; `application.properties:16-17` fuerza `MySQL8Dialect`, obsoleto en Hibernate 6.
- `backup_db.sql` es un volcado de 2026-05-17 con tipos y esquema antiguos (sin `pedido_id` en ventas, sin `direccion`); restaurarlo destruiría el esquema actual.
- Seeds no idempotentes ni aritméticamente consistentes: venta 5 totaliza 21.980 pero sus ítems suman 21.970; precio de producto 4 difiere entre catálogo e ítems; `stock_bajo` lista un material con stock 100 > mínimo 15; bloqueo de vacaciones duplicado entre scripts; “Fiestas Patrias” en julio.
- No hay Flyway/Liquibermigration: el esquema real diverge silenciosamente de los `.sql`.

### 4.6. Documentación, pruebas y tooling

- README desactualizado:
  - Bug #4 (stock-bajo) ya corregido en código.
  - Bugs #1 y #3 (prefijos `/api/v1`) ya corregidos.
  - Bug #2 corregido en properties, pero queda fallback 8084 en Java.
  - `README.md:382` habla de “bugs #1 y #2 descritos arriba” cuando la sección está abajo.
  - URL de clonado apunta a otro repositorio (`lugrossolli-ux/perlitas_de_eucarion`).
  - Afirma que “cada microservicio valida los tokens JWT” (`:432`) y que VENDEDOR no elimina (`:419`), pero el código no cumple.
  - Dice que galería sube imágenes y que reportes consolidan datos con folito temporal; ninguna de las dos cosas es cierta.
- Cobertura real inferior a la declarada: `reportes`, `service_calendario` y `gateway` solo tienen `contextLoads`; faltan tests de `PedidoMaterialController`, `ProveedorMaterial*`, `CategoriaGasto*` y DELETE de galería; los tests de controller desactivan filtros de seguridad; los `contextLoads` requieren MySQL local, por lo que fallan en CI sin XAMPP.
- `.gitignore` contiene solo `*.zip` repetido, está en UTF-16 LE con BOM y no ignora `target/`, `.env`, IDE ni logs.
- `.vscode/launch.json` referencia un `.env` inexistente, tiene un typo `com.example.provedores.ProvedoresApplication` y dos configuraciones duplicadas de Ventas, una con `mainClass` inválida; `cwd` apunta a la raíz en lugar del módulo.
- No hay aggregator POM, Docker Compose, CI ni profiles de configuración: arrancar el sistema exige 12 comandos manuales.

---

## 5. Estado real de los “bugs conocidos” del README

| Bug documentado | Estado verificado en código |
|---|---|
| `reportes` usa prefijos `/api/v1/...` inexistentes | **Corregido**: URIs sin prefijo en `ReporteService.java:59,66,89,112` |
| `reportes` apunta al puerto 8084 en vez de 8085 | **Corregido en properties** (`application.properties:23`); **queda fallback 8084** en `WebClientConfig.java:20` |
| `service_calendario` usa prefijos `/api/v1/...` | **Corregido**: `/productos/{id}` y `/pedidos/{id}` en `ProduccionService.java:127,141` |
| `materiales/stock-bajo` filtra `stockActual < 0` | **Corregido**: `MaterialRepository.java:12` compara contra `stockMinimo` |

Los síntomas pueden persistir, pero por otras causas: autenticación no propagada entre servicios, `onErrorResume` silencioso, sin timeouts y filtros de fecha no aplicados.

---

## 6. Reglas de negocio: documentación vs implementación

| Regla | Implementación real |
|---|---|
| Pedidos avanzan en orden sin retroceso | Solo en `PATCH`; evadible por `PUT` (C5) y sin control concurrente |
| Producción sigue `programado → en_proceso → finalizado`, cancelable | Solo en `PATCH`; evadible por `PUT` y sin revalidar bloqueos |
| No se puede programar producción si hay bloqueo | Solo al crear; no al actualizar ni al crear bloqueos |
| No reasignar productos a categoría inactiva | Implementado parcialmente (`activo == 0`); otros valores pasan |
| Total de venta calculado automáticamente | Calculado, pero con precios del cliente y sin consultar catálogo |
| Descuento de stock en venta | No implementado, aunque Swagger lo promete |
| Pedidos verifican producto antes de crear | Método presente pero nunca llamado; datos de pedido ni siquiera incluyen `productoId` |
| Cada microservicio valida JWT | Falso: solo lee cabeceras `X-User-*` (C2) |
| Cliente solo GET, Vendedor sin DELETE | Gateway sí, servicios no (GET negado a CLIENTE, PATCH sin regla, DELETE permitido a VENDEDOR en pedidos) |
| Galería sube imágenes | Falso: solo guarda una URL |
| Reportes por período y consolidados | Falso: ignora fechas y solo devuelve conteos |

---

## 7. Plan de acción priorizado

### P0 — Corregir antes de cualquier despliegue
1. Eliminar `roles` del DTO público de registro, asignar un rol por defecto en servidor y crear un endpoint admin protegido para altas con roles (`AuthService.java:40-49`).
2. Reemplazar la confianza en `X-User-*` por validación real de JWT en cada servicio (librería común) o, como mínimo, por un token interno firmado con secreto distinto + bloqueo de red de los puertos directos. Quitar del README la recomendación de acceso directo.
3. Sustituir `startsWith`/`contains` en `AuthenticationFilter.shouldNotFilter` por matchers exactos y añadir `RemoveRequestHeader` para `X-User-*` en todas las rutas del gateway. Añadir test de regresión para `/materiales/v3/api-docs`.
4. Mover `jwt.secret` a variable de entorno/secret manager, rotar la clave comprometida y usar valores distintos por entorno.
5. Bloquear cambios de estado por `PUT` en pedidos y producción; centralizar las transiciones en un único método del dominio con `@Transactional` y `@Version`/bloqueo pesimista.
6. Romper el ciclo `Producto ↔ ProductoMaterial` con `@JsonIgnore`/DTOs y eliminar `fetch = EAGER`.
7. Refactorizar `crearVenta`: un único `save` con la relación asignada antes de persistir, `@Transactional`, precios obtenidos del catálogo y validación de stock.

### P1 — Correcciones funcionales
8. Configurar `connectTimeout`/`responseTimeout`, `retry` acotado y circuit breaker en todos los WebClient; sustituir los `.block()` en bucle por llamadas batch.
9. Propagar identidad (JWT o token de servicio) en las llamadas `productos→materiales`, `ventas→productos`, `pedidos→productos`, `gastos→proveedores`, `reportes→*` y `calendario→*`; eliminar `onErrorResume` que convierte fallos en `false`, `null`, “no disponible” o conteos `0`.
10. Aplicar el filtro `desde/hasta` en `generarReporteVentas` (o renombrar el endpoint a “conteo global”) y corregir el fallback 8084.
11. Implementar reserva/descuento de stock con contrato atómico (endpoint en `materiales` o saga/outbox) y validar `abono <= total`.
12. Unificar la matriz de roles en una sola fuente, cubriendo explícitamente `PATCH` y `OPTIONS`; separar 401 de 403; mensajes genéricos de error; logging y correlation ID.
13. Añadir API de vinculación producto-categoría, proteger el borrado de categorías con hijos y hacer la reasignación transaccional, idempotente y con constraint único.
14. Validar existencia de proveedor/material/categoría en relaciones; usar `BigDecimal` y `@Positive`/`@DecimalMin`; bloquear borrados referenciados.
15. Decidir si galería sube archivos o solo registra URLs; en el primer caso implementar multipart, límites, almacenamiento y borrado del archivo.

### P2 — Consistencia y datos
16. Elegir MySQL u Oracle como fuente canónica, generar migraciones con Flyway/Liquibase, eliminar FKs cross-DB y añadir tablas de seguridad; retirar o marcar `backup_db.sql` como histórico.
17. Regenerar seeds con importes y estados válidos, idempotentes y con nombres de BD unificados.
18. Paginación en todos los listados; Bean Validation completa (enum de estados, `@Positive`, fechas coherentes); DTOs de request/response; `ErrorResponse` único con timestamp ISO-8601.
19. CORS único y restrictivo, `OPTIONS` permitido en el gateway, TLS en gateway y entre servicios, rate limiting en login.
20. Reescribir README: quitar bugs resueltos, corregir URL de clonado, nombres de BD, ejemplo de PATCH de producción, cobertura y afirmaciones falsas sobre JWT/galería/reportes.

### P3 — Sostenibilidad
21. Extraer seguridad, errores, OpenAPI y CORS a un módulo `commons`/starter compartido; eliminar las copias en 12 módulos.
22. Constructor injection, `ddl-auto=validate` en producción, secretos fuera del repo, aggregator POM, Docker Compose y CI.
23. Arreglar `.gitignore` (UTF-8), crear `.env.example` y corregir `.vscode/launch.json`.
24. Tests de integración/contrato (Testcontainers o H2), tests de seguridad con filtros activados, de transiciones concurrentes, de timeouts y de queries JPQL reales; completar los tests faltantes.
25. Actualizar `jjwt` a 0.12.x y migrar la API; revisar codificación de caracteres en archivos con textos dañados (`autenticaci?n`, `Categor?a`, etc.).

---

## 8. Conclusión

El proyecto tiene una base sólida desde el punto de vista estructural: dominio bien dividido, contratos REST uniformes, documentación abundante y una arquitectura de microservicios coherente en papel. Sin embargo, **la seguridad es decorativa** (C1-C4), **varias reglas de negocio son evadibles o no están implementadas** (C5, stock, verificación de productos, ranged de reportes), y **la documentión describe un sistema más robusto del que realmente existe**.

La prioridad no es añadir funcionalidad nueva, sino cerrar la frontera de confianza, hacer cumplir las reglas de negocio en el servidor y sincronizar código, datos y documentación. Con esos tres frentes corregidos, el proyecto pasaría de una demo frágil a una base razonablemente defendible.
