# Estado del proyecto

**Sesión actual:** 0.2 completada → siguiente: **0.3 Escritorio base**
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

## Decisiones vigentes
- Dominio API: `apipos.comercializadora-neymar.com`. VPS con Nginx existente: la API escucha en
  `127.0.0.1:8080` y se agrega un `server` a Nginx.
- Repo público en GitHub; despliegue con GitHub Actions al hacer push.
- Facturas en dos formatos: carta y tirilla 80 mm. Sin migración de datos de 2020.
- Solo Maven Central (el host `plugins-artifacts.gradle.org` está bloqueado en la red de Claude):
  no usar plugins que solo existan en el portal de Gradle.

## Pendiente del usuario
- Crear el repo público en GitHub y hacer el primer push (antes de 0.4).

## Preguntas abiertas
- (ninguna)
