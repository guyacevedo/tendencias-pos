# F1 — Identidad y estructura

## 1.1 Identidad en el servidor
**Hacer**
- Tablas: `app_user` (username único, nombre, hash, activo, intentos fallidos, bloqueado_hasta),
  `role`, `user_role`, `refresh_token` (hash del token, expira, revocado). Roles: `ADMIN`, `CASHIER`.
- Hash Argon2id (`Argon2PasswordEncoder` de Spring Security); longitud mínima 10.
- `POST /api/auth/login` → access JWT (15 min, HS256 con `JWT_SECRET` ≥ 32 bytes) + refresh token opaco
  (8 h, rotación en cada uso). `POST /api/auth/refresh`, `POST /api/auth/logout` (revoca).
- Bloqueo 15 min tras 5 intentos fallidos; respuesta igual para usuario inexistente y clave errada.
- Spring Security stateless como resource server JWT; `@PreAuthorize` por rol; CORS desactivado.
- Usuarios: `GET/POST/PUT /api/users` (ADMIN), restablecer clave, desactivar (nunca borrar).
- Primer administrador: creado al arrancar si no hay usuarios, con `ADMIN_INITIAL_PASSWORD` del entorno
  y obligación de cambiarla en el primer login.
- `audit_log` para login, logout, cambios de usuario.
**Aceptación:** pruebas de login correcto/incorrecto/bloqueo, refresh rotado, acceso denegado a CASHIER
en `/api/users`, token vencido → 401 Problem Details.

## 1.2 Login y estructura del escritorio
**Hacer**
- Pantalla de login (usuario, clave, mostrar/ocultar, Enter envía, error claro, bloqueo informado).
- `Session` en memoria con tokens; renovación automática del access token; al expirar el refresh →
  volver al login. Nada se guarda en disco.
- Menú lateral según rol; cerrar sesión; cambio de clave obligatorio en primer ingreso.
- Configuración de la tienda: `store_settings` (nombre, NIT, dirección, teléfono, formato de factura
  carta/80 mm) con `GET/PUT /api/settings` (PUT solo ADMIN) y su pantalla.
- Administración de usuarios (lista + formulario) para ADMIN.
**Aceptación:** login con admin inicial, cambio de clave, crear un cajero y entrar con él viendo menos
opciones; presentadores con pruebas unitarias.
