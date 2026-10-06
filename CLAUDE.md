# CLAUDE.md — Tendencias POS 2.0

Guía para Claude. Leer SIEMPRE este archivo y `docs/STATE.md` al iniciar una sesión; luego solo la
especificación de la sesión en `docs/phases/` y los archivos que se van a tocar.

## Proyecto
POS de escritorio para una tienda de calzado (una tienda, varios usuarios). Cliente Swing → API REST
(HTTPS) → PostgreSQL. Requiere internet siempre; sin modo offline. Plan completo: doc "Plan de
reconstrucción — Tendencias Shoes POS 2.0".

## Módulos
- `contracts` — records DTO compartidos. Sin dependencias de framework.
- `server` — Spring Boot 4.1, monolito modular. Paquete base `com.guycode.tendenciaspos`.
  Un paquete por módulo de negocio: `identity, catalog, inventory, parties, sales, purchasing, cash,
  billing, shared`. Dentro: `domain/` (sin Spring), `application/` (casos de uso),
  `adapter/in/web`, `adapter/out/persistence`.
- `desktop/ui-kit` — tema FlatLaf y componentes reutilizables.
- `desktop/app` — pantallas en patrón MVP (vista pasiva + presentador que se prueba sin Swing).

## Comandos (shell Linux de Claude)
```bash
source scripts/claude-env.sh   # instala/usa JDK 21 y define tpos_build
tpos_build                     # spotlessApply + build en silencio; solo imprime errores
./gradlew :server:test -q --tests '*NombreTest'   # una prueba puntual
```
Docker no existe en el shell de Claude: las pruebas con Testcontainers se marcan `@Tag("it")`, se
excluyen localmente y corren en GitHub Actions.

## Reglas de código
- Java 21: records para DTO y value objects, `sealed` para resultados, `var` solo si el tipo es obvio.
- Dinero: `BigDecimal` + `NUMERIC(14,2)`. Fechas: `Instant`/`timestamptz`; zona `America/Bogota` solo al mostrar.
- SQL: nunca concatenar. Solo Spring Data/JPA o parámetros con nombre.
- Toda operación que cambie stock, caja o documentos va en UNA transacción (`@Transactional` en el caso de uso).
- Errores HTTP como Problem Details (RFC 9457) con `code` estable (`STOCK_INSUFICIENTE`).
- `domain` no importa Spring ni JPA (lo vigila ArchUnit). Un módulo no usa paquetes internos de otro.
- Swing: nada de `setBounds` ni `SystemColor`; MigLayout. Llamadas a la API fuera del EDT; volver con
  `SwingUtilities.invokeLater`.
- Secretos solo por variables de entorno. Nada sensible en el repo (es público).
- Compilación con `-Werror`; formato Palantir vía Spotless (no formatear a mano).
- Nombres de código en inglés; textos de UI, mensajes de error y docs en español.

## Ahorro de tokens (obligatorio)
- No leer archivos que no se van a tocar; usar `grep -rn` para ubicar.
- Editar con cambios puntuales; no reescribir archivos completos ni releer lo recién escrito.
- Salidas de build/pruebas siempre filtradas (`tpos_build`).
- Nuevos módulos CRUD: copiar el patrón de `catalog/category` (desde la sesión 2.1) con `scripts/new-module`.
- Al terminar la sesión: actualizar `docs/STATE.md` (máx. 60 líneas) y hacer commit.

## Git
Conventional Commits en español (`feat(sales): …`). Un commit por sesión como mínimo. Pie del commit:
`Co-Authored-By: Claude <noreply@anthropic.com>`.

## Notas técnicas (Spring Boot 4 / Spring 7 / Testcontainers 2)
- `@WebMvcTest` está en `org.springframework.boot.webmvc.test.autoconfigure`; usar `MockMvcTester` (AssertJ).
  En slices, importar explícitamente `@Import(GlobalExceptionHandler.class)` y
  `@EnableConfigurationProperties(TposProperties.class)` si el controlador los necesita.
- Personalizar errores del framework en `GlobalExceptionHandler.createResponseEntity` (en
  `handleExceptionInternal` el cuerpo aún puede ser `null`).
- Testcontainers 2: `org.testcontainers.postgresql.PostgreSQLContainer` + `@ServiceConnection`.
- Jackson 3 (`tools.jackson.*`) en servidor y escritorio.
- Verificación contra PostgreSQL real sin Docker: `./gradlew :server:bootJar`, llevar el jar al entorno
  de Claude en la nube (tiene PostgreSQL) y correrlo con el perfil `dev`.
