# Estado del proyecto

**Sesión actual:** 1.2 completada → siguiente: **2.1** (F2-catalogo-inventario.md)
**Última actualización:** 2026-10-06

## Hecho
- F0 (0.1–0.4): Gradle multi-proyecto + Spotless/`-Werror`; servidor (JPA, Flyway, perfiles `dev`/`prod`,
  Problem Details, `GET /api/version`, ArchUnit, ITs `@Tag("it")`); escritorio base (FlatLaf, `MainFrame`,
  `ApiClient`, `UiExecutor`, `OfflineBar`); CI, imagen arm64 y despliegue al VPS.
- 1.1 — Identidad en el servidor: `V2__identity`, JWT HS256 (15 min) + refresh de un solo uso (8 h),
  Argon2id, bloqueo 5×/15 min, `PasswordChangeRequiredFilter`, auditoría, `/api/auth/*`, `/api/users/*`
  (ADMIN) y admin inicial con `ADMIN_INITIAL_PASSWORD`.
- 1.2 — Sesión en el escritorio y configuración de la tienda.
  - Servidor: `V3__store_settings` (fila única `id = 1`) y `GET/PUT /api/settings` en `shared/settings`
    (lee cualquier autenticado; escribe solo ADMIN, auditado `SETTINGS_UPDATED`). Solo el nombre es
    obligatorio; NIT, dirección y teléfono pueden quedar vacíos.
  - `SessionManager`: tokens solo en memoria, renueva el access con 60 s de margen, renovaciones
    serializadas (el refresh es de un solo uso) y vuelve al ingreso ante un 401 o un refresh rechazado.
    `ApiClient.Authorization` pone la cabecera por ruta y avisa el 401; `ApiException.details()` y
    `fieldErrors()` exponen los extras del Problem Details.
  - Pantallas MVP: `LoginFrame` (clave con mostrar/ocultar, Enter envía, bloqueo con hora, cambio de
    clave obligatorio), `MainFrame` con menú por rol (`Route.visibleFor`), usuario y cerrar sesión,
    `UsersPanel` (lista + formulario, restablecer clave, activar/desactivar) y `SettingsPanel`.
    Nuevos: `Fields` (ui-kit), `Dates` (hora de Bogotá) y `ApiErrors` (`core`).
  - 109 pruebas unitarias y 5 ITs verdes. Verificado con Docker local: ITs contra PostgreSQL 17, la API
    con curl y el cliente del escritorio contra la API `dev` (cambio obligatorio de clave, 403 del
    cajero, validación por campo, refresh revocado al salir); pantallas revisadas en capturas.

## Decisiones vigentes
- Sesión = 8 h absolutas desde el login; el refresh rota en cada uso pero no extiende el fin. Reusar un
  refresh ya usado revoca todas las sesiones del usuario (por eso el escritorio las serializa).
- Login: usuario inexistente, inactivo o clave errada → `INVALID_CREDENTIALS` (401, hash señuelo);
  bloqueo → `ACCOUNT_LOCKED` (423, `lockedUntil`). Siempre queda ≥ 1 ADMIN activo (`LAST_ADMIN`) y nadie
  se desactiva a sí mismo. Usuarios nuevos y claves restablecidas obligan a cambiar la clave. Códigos de
  error en inglés (precedente de `ErrorCode`).
- El escritorio no guarda nada en disco salvo el tema; la sesión vive en memoria y se pierde al cerrar.
- Menú del cajero: Inicio, Ventas, Clientes y Caja. Todo lo demás es solo del administrador.
- Dominio API: `apipos.comercializadora-neymar.com` (DNS listo). VPS Oracle **ARM** (Ubuntu 24.04) con
  proxy de borde en Docker (`/opt/pos-neymar-edge`, `pos-cn-proxy`): la API no publica puertos, el proxy
  la alcanza como `tpos-api:8080` por la red externa `red-tpos`. Imagen arm64 (`ubuntu-24.04-arm`).
- Repo público en GitHub; despliegue con GitHub Actions al hacer push.
- Facturas en dos formatos: carta y tirilla 80 mm. Sin migración de datos de 2020.
- Solo Maven Central (el host `plugins-artifacts.gradle.org` está bloqueado en la red de Claude):
  no usar plugins que solo existan en el portal de Gradle.

## Pendiente del usuario
- **Antes del próximo push a `main`:** poner `JWT_SECRET` y `ADMIN_INITIAL_PASSWORD` en el `.env` del
  VPS (`docs/deploy.md` §4). Sin `JWT_SECRET` el despliegue falla en `compose`.
- Probar la app en el Mac: `docker compose -f deploy/compose.dev.yml up -d`, `./gradlew :server:bootRun`
  y `./gradlew :desktop:app:run`; entrar con `admin` / `admin-dev-123` (pedirá cambiar la clave).

## En producción
- 2026-10-06: CI y Deploy en verde; `https://apipos.comercializadora-neymar.com/api/version` responde.
  Llave de despliegue rotada; proxy de borde conectado a `red-tpos` (también en su compose). La spec de
  0.4 asumía Nginx en el host con `127.0.0.1:8080`; se cambió a red Docker compartida.

## Notas de entorno
- Nunca usar `**/out` en `.gitignore` ni `.dockerignore`: se tragaba `adapter/out/`.
- En este Mac hay Docker y JDK 21: `build`, `:server:integrationTest` y `compose.dev.yml` corren bien.
- Maven Central responde 429 intermitente en la nube: reintentar o poner `repo1.maven.org` primero en un
  init script de `~/.gradle/init.d`.

## Preguntas abiertas
- (ninguna)