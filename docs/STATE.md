# Estado del proyecto

**Sesión actual:** 0.4 completada → siguiente: **1.1** (F1-identidad.md)
**Última actualización:** 2026-10-06

## Hecho
- 0.1 — Gradle 9.8 multi-proyecto (contracts, server, desktop:ui-kit, desktop:app), catálogo de
  versiones, convenciones en `build.gradle.kts` raíz, Spotless + `-Werror`, CLAUDE.md, specs de fases,
  script `scripts/claude-env.sh`, licencia MIT.
- 0.2 — Servidor: JPA, Flyway (`V1__base`: `audit_log`), PostgreSQL, perfiles `dev`/`prod`
  (`prod` exige `DB_URL`, `DB_USER`, `DB_PASSWORD`), Problem Details con `code` (`ErrorCode`,
  `BusinessException`, `GlobalExceptionHandler`), `GET /api/version`, springdoc solo en `dev`,
  ArchUnit, `ApplicationIT` (`@Tag("it")`, tarea `integrationTest`), `deploy/compose.dev.yml`.
  Verificado contra PostgreSQL real: health UP, versión, 404 Problem Details, migración aplicada.
- 0.3 — Escritorio: `ui-kit` con `Theme` (FlatLaf + `TposTheme.properties`, Inter, claro/oscuro
  guardado en Preferences), `Icons` (Ikonli MDI2), `Toast`, `LoadingOverlay`, `MoneyField`/`MoneyFormat`
  (COP: miles con punto, oculta ",00"). `app`: `MainFrame` con `SideMenu` (solo iconos < 1100 px) y `CardLayout` por
  `Route`; `ApiClient` (HttpClient + Jackson 3, `X-Client-Version`, Problem Details → `ApiException`);
  `UiExecutor` (hilos virtuales → EDT); `ConnectionMonitor` + `OfflineBar` (sonda cada 5 s);
  `HomePresenter` muestra versiones. URL de la API: `-Dtpos.api.url` o `TPOS_API_URL`
  (por defecto `http://localhost:8080`). 29 pruebas en verde; verificado con capturas contra el servidor.
- 0.4 — `.github/workflows/ci.yml` (build + `integrationTest`, reportes como artefacto), `deploy.yml`
  (tras CI verde en `main`: imagen `ghcr.io/<repo>/api:<sha12>` y `:latest` → SSH → compose pull/up
  `--wait` → prueba `/api/version`; sin secretos solo publica la imagen), `dependabot.yml`,
  `server/Dockerfile` (3 etapas, capas Spring Boot, temurin 21 alpine, usuario `tpos`, `-Xmx512m`,
  healthcheck), `.dockerignore`, `deploy/compose.prod.yml` (red `db` interna, API solo en `red-tpos`),
  `deploy/.env.example`, `deploy/nginx/apipos.conf.template`, `docs/deploy.md`. `prod` usa
  `forward-headers-strategy: native`. Verificado: actionlint, `compose config`, jar extraído por capas
  con perfil `prod` contra PostgreSQL real. No verificado aquí: `docker build` (Docker Hub bloqueado).

## Decisiones vigentes
- Dominio API: `apipos.comercializadora-neymar.com` (DNS listo). VPS Oracle **ARM** (Ubuntu 24.04) con
  proxy de borde en Docker (`/opt/pos-neymar-edge`, `pos-cn-proxy`): la API no publica puertos, el
  proxy la alcanza como `tpos-api:8080` por la red externa `red-tpos`. Imagen arm64 (runner
  `ubuntu-24.04-arm`).
- Repo público en GitHub; despliegue con GitHub Actions al hacer push.
- Facturas en dos formatos: carta y tirilla 80 mm. Sin migración de datos de 2020.
- Solo Maven Central (el host `plugins-artifacts.gradle.org` está bloqueado en la red de Claude):
  no usar plugins que solo existan en el portal de Gradle.

## Pendiente del usuario
- Probar en el Mac: `./gradlew :desktop:app:run` con el servidor `dev` encendido y luego apagado.
- Opcional: apuntar el escritorio a producción con `TPOS_API_URL=https://apipos.comercializadora-neymar.com`.

## En producción
- 2026-10-06: CI y Deploy en verde; `https://apipos.comercializadora-neymar.com/api/version` responde.
  Llave de despliegue rotada; proxy de borde conectado a `red-tpos` (también en su compose).
- La spec de 0.4 (F0) asumía Nginx en el host con `127.0.0.1:8080`; se cambió a red Docker compartida.

## Notas de entorno
- La carpeta del Mac no permite borrar por defecto: compilar en una copia (`rsync` a `~/tpos-copy`)
  y pedir permiso de borrado antes de usar git (deja `.lock`).

## Preguntas abiertas
- (ninguna)
