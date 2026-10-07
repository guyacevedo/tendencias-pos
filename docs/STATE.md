# Estado del proyecto

**Sesión actual:** 1.1 completada → siguiente: **1.2** (F1-identidad.md)
**Última actualización:** 2026-10-06

## Hecho
- F0 (0.1–0.4): Gradle multi-proyecto + Spotless/`-Werror`; servidor (JPA, Flyway, perfiles `dev`/`prod`,
  Problem Details con `code`, `GET /api/version`, ArchUnit, ITs `@Tag("it")`); escritorio base (FlatLaf,
  `MainFrame`/`SideMenu`, `ApiClient`, `UiExecutor`, `OfflineBar`); CI, imagen arm64 y despliegue al VPS.
- 1.1 — Identidad en el servidor. `V2__identity` (`role`, `app_user`, `user_role`, `refresh_token`).
  `shared/security`: `SecurityConfig` (stateless, JWT HS256 `JWT_SECRET` ≥ 32 bytes, sin CSRF/CORS,
  `@EnableMethodSecurity`), 401/403 como Problem Details (`TOKEN_EXPIRED` si venció),
  `PasswordChangeRequiredFilter` (con `pwd_change` solo `/api/auth/{me,password,logout}`). `shared/audit/AuditLog`
  (JdbcClient, jsonb). `ClockConfig` (Clock inyectable). `identity`: dominio puro (`User`, `LockoutPolicy` 5×/15 min,
  `PasswordPolicy` 10–128, `Username` en minúsculas), puertos + adaptadores JPA, `AuthService` (resultados
  `sealed` para que el fallo quede guardado), `UserAdminService`, `InitialAdminBootstrap`. Endpoints:
  `POST /api/auth/{login,refresh,logout,password}`, `GET /api/auth/me`, `GET/POST /api/users`,
  `GET/PUT /api/users/{id}`, `POST /api/users/{id}/{password,deactivate,activate}` (ADMIN). DTO en
  `contracts.identity`. 42 pruebas del servidor (unitarias + slices web); `IdentityFlowIT` verificado contra PostgreSQL 16 real
  (sin Docker) y el jar `dev` probado con curl.

## Decisiones vigentes
- Sesión = 8 h absolutas desde el login; el refresh rota en cada uso pero no extiende el fin. Reusar un
  refresh ya usado revoca todas las sesiones del usuario → el escritorio debe serializar las renovaciones.
- Login: usuario inexistente, inactivo o clave errada → `INVALID_CREDENTIALS` (401, hash señuelo).
  Bloqueo → `ACCOUNT_LOCKED` (423, `lockedUntil`). Desactivar revoca refresh; el access vive ≤ 15 min.
- Siempre queda ≥ 1 ADMIN activo (`LAST_ADMIN`); nadie se desactiva a sí mismo. Usuarios nuevos y claves
  restablecidas obligan a cambiar la clave. Códigos de error en inglés (precedente de `ErrorCode`).
- Dominio API: `apipos.comercializadora-neymar.com` (DNS listo). VPS Oracle **ARM** (Ubuntu 24.04) con
  proxy de borde en Docker (`/opt/pos-neymar-edge`, `pos-cn-proxy`): la API no publica puertos, el
  proxy la alcanza como `tpos-api:8080` por la red externa `red-tpos`. Imagen arm64 (runner
  `ubuntu-24.04-arm`).
- Repo público en GitHub; despliegue con GitHub Actions al hacer push.
- Facturas en dos formatos: carta y tirilla 80 mm. Sin migración de datos de 2020.
- Solo Maven Central (el host `plugins-artifacts.gradle.org` está bloqueado en la red de Claude):
  no usar plugins que solo existan en el portal de Gradle.

## Pendiente del usuario
- **Antes del próximo push a `main`:** agregar al `.env` del VPS `JWT_SECRET` y `ADMIN_INITIAL_PASSWORD`
  (ver `docs/deploy.md` §4). Sin `JWT_SECRET` el despliegue falla en `compose`.
- Probar en el Mac: `./gradlew :desktop:app:run` con el servidor `dev` encendido y luego apagado.
- Opcional: apuntar el escritorio a producción con `TPOS_API_URL=https://apipos.comercializadora-neymar.com`.

## En producción
- 2026-10-06: CI y Deploy en verde; `https://apipos.comercializadora-neymar.com/api/version` responde.
  Llave de despliegue rotada; proxy de borde conectado a `red-tpos` (también en su compose).
- La spec de 0.4 (F0) asumía Nginx en el host con `127.0.0.1:8080`; se cambió a red Docker compartida.

## Notas de entorno
- `.gitignore` ignoraba todo `out/` (incluido `adapter/out/`); ahora solo `/out/` y `*/out/` de módulos.
- La carpeta del Mac no permite borrar por defecto: pedir permiso de borrado antes de usar git (deja `.lock`).
- 1.1 se compiló en la nube (clon de GitHub, JDK 21 y PostgreSQL 16 locales). Maven Central responde 429
  intermitente: reintentar, o init script en `~/.gradle/init.d` con `repo1.maven.org` primero.

## Preguntas abiertas
- (ninguna)
