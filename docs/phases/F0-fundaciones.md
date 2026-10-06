# F0 — Fundaciones

## 0.1 Esqueleto ✅ (completada)
Gradle multi-proyecto, catálogo de versiones, convenciones, CLAUDE.md, STATE.md, specs, licencia.

## 0.2 Servidor base
**Hacer**
- Dependencias: data-jpa, validation, flyway (+ flyway-database-postgresql), postgresql, springdoc.
- `deploy/compose.dev.yml` con PostgreSQL 17 (puerto 5432 solo local) para desarrollo en el Mac.
- `application.yml` con perfiles `dev` y `prod`; todo por variables de entorno (`DB_URL`, `DB_USER`,
  `DB_PASSWORD`, `JWT_SECRET`…), sin valores sensibles por defecto.
- Flyway `V1__base.sql`: tabla `audit_log` y extensión `pgcrypto` si hace falta.
- `shared/web`: `@RestControllerAdvice` → Problem Details con `code`; `ErrorCode` enum.
- `GET /api/version` → `ApiVersion` (contracts), con `minClientVersion` desde configuración.
- ArchUnit: `domain` sin Spring/JPA; módulos sin ciclos.
- Pruebas: `@WebMvcTest` del endpoint de versión; prueba de integración `@Tag("it")` con Testcontainers
  que levanta el contexto y aplica Flyway. Excluir `it` del `test` local; tarea `integrationTest`.
**Aceptación:** `tpos_build` verde; OpenAPI en `/swagger-ui.html` (solo perfil dev); error 404 devuelve
Problem Details.

## 0.3 Escritorio base
**Hacer**
- `ui-kit`: tema FlatLaf propio (`TposTheme.properties`, colores de marca, fuente Inter si es libre de
  usar, modo claro/oscuro), `Icons` (Ikonli MDI2), `Toast`, `LoadingOverlay`, `MoneyField`.
- `app`: ventana principal con menú lateral colapsable (iconos solos < 1100 px) y área de contenido con
  `CardLayout`; navegación por `Route` enum.
- `ApiClient` sobre `java.net.http.HttpClient` + Jackson 3: base URL configurable, timeouts, mapeo de
  Problem Details a `ApiException(code, message)`, cabecera `X-Client-Version`.
- `UiExecutor`: ejecuta tareas fuera del EDT (hilos virtuales) y entrega el resultado en el EDT.
- Barra "Sin conexión con el servidor" que se muestra al fallar la red y se oculta al recuperarse.
- Pantalla de inicio que llama `/api/version` y muestra la versión del servidor.
**Aceptación:** la app abre en el Mac del usuario, se redimensiona sin romper el layout, muestra la
versión del servidor local y el aviso de desconexión si la API está apagada. Pruebas de `ApiClient` con
un `HttpServer` del JDK.

## 0.4 CI y despliegue base
**Hacer**
- `.github/workflows/ci.yml`: en PR y push a `main`: JDK 21, cache Gradle, `./gradlew build integrationTest`,
  reporte de pruebas como artefacto.
- `server/Dockerfile` multi-stage (JRE 21 alpine o distroless, usuario no root, `-Xmx512m`).
- `deploy/compose.prod.yml`: `api` (publica `127.0.0.1:8080:8080`) + `postgres` (sin puertos, volumen,
  healthcheck) + red interna. `deploy/.env.example`.
- `deploy/nginx/apipos.conf`: `server` para `apipos.comercializadora-neymar.com` con proxy a
  `127.0.0.1:8080`, cabeceras de seguridad, `client_max_body_size 1m`; instrucciones de certbot.
- `.github/workflows/deploy.yml`: en push a `main` (si CI pasa) construir imagen → GHCR → SSH al VPS →
  `docker compose pull && up -d`. Secretos: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`.
- `.github/dependabot.yml` (gradle, github-actions, docker).
- `docs/deploy.md`: pasos únicos en el VPS (usuario deploy, carpeta, `.env`, DNS, certbot).
**Aceptación:** CI verde en GitHub; con los secretos configurados, `GET https://apipos…/api/version`
responde. (El primer despliegue lo ejecuta el usuario siguiendo `docs/deploy.md`.)
