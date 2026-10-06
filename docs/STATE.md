# Estado del proyecto

**Sesión actual:** 0.3 completada → siguiente: **0.4 CI y despliegue base**
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

## Decisiones vigentes
- Dominio API: `apipos.comercializadora-neymar.com`. VPS con Nginx existente: la API escucha en
  `127.0.0.1:8080` y se agrega un `server` a Nginx.
- Repo público en GitHub; despliegue con GitHub Actions al hacer push.
- Facturas en dos formatos: carta y tirilla 80 mm. Sin migración de datos de 2020.
- Solo Maven Central (el host `plugins-artifacts.gradle.org` está bloqueado en la red de Claude):
  no usar plugins que solo existan en el portal de Gradle.

## Pendiente del usuario
- Crear el repo público en GitHub y hacer el primer push (antes de 0.4).
- Probar en el Mac: `./gradlew :desktop:app:run` con el servidor `dev` encendido y luego apagado.

## Preguntas abiertas
- (ninguna)
