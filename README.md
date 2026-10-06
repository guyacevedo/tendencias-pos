# Tendencias POS 2.0

Punto de venta de escritorio para una tienda de calzado: inventario por talla, compras, ventas de contado y a crédito, caja y facturas. Reescritura en 2026 del sistema que desarrollé en 2020 para Tendencias Shoes (Chinú, Sucre).

**Estado:** en construcción. Ver [docs/STATE.md](docs/STATE.md).

## Arquitectura

- `desktop/` — app de escritorio en Java 21 + Swing con FlatLaf y MigLayout.
- `server/` — API REST en Spring Boot 4 con PostgreSQL; la única pieza que toca la base de datos.
- `contracts/` — DTO compartidos entre ambos.

## Requisitos

- JDK 21
- Docker (para la base de datos local y las pruebas de integración)

## Comandos

```bash
./gradlew build                     # compila, formatea y prueba todo
./gradlew :server:bootRun           # levanta la API en http://localhost:8080
./gradlew :desktop:app:run          # abre la app de escritorio
```

## Despliegue

CI y despliegue con GitHub Actions a un VPS con Docker y Nginx: ver [docs/deploy.md](docs/deploy.md).

## Licencia

MIT
