# Integración local (Evaluación 3)

1. Iniciar MySQL/MariaDB de XAMPP (3306).
2. Desde la raíz del backend: `powershell -ExecutionPolicy Bypass -File .\iniciar.ps1`. Usar `-Todos` para los 12 servicios. Usar `-Reiniciar` para reiniciar procesos Java del proyecto.
3. Desde el frontend: copiar `.env.example` a `.env` (VITE_API_URL=/api/v1), `npm install` y `npm run dev`.
4. Gateway: http://localhost:9090/api/v1; Swagger: http://localhost:9090/swagger-ui.html. El proxy Vite evita CORS durante el desarrollo local.
5. Login: JWT de texto plano, firmado y validado en Gateway; expira en 24 horas y requiere nuevo login. El Gateway comprueba la cuenta activa y su rol actual para peticiones protegidas. ADMIN y VENDEDOR. Los microservicios internos escuchan en loopback; su confianza en cabeceras internas no es un diseño de despliegue público. No publicar los puertos internos.
6. Para validar: `npm test` y `npm run build` en frontend; `mvnw.cmd test` en cada servicio backend. Pruebas reales pendientes de ejecución en el entorno local.

Alcance actual: gestión interna. La tienda pública sin cuenta no está implementada en esta versión; confirmar alcance con docente.
