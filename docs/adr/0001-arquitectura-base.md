# ADR 0001 — Cliente de escritorio + API propia + monolito modular

**Estado:** aceptada (2026-10-05)

## Contexto
El sistema de 2020 conectaba Swing directo a MySQL local, con SQL concatenado y sin transacciones.
La versión 2.0 debe tener la base de datos en la nube, varios usuarios y buena seguridad.

## Decisión
- El escritorio nunca habla con la base de datos: usa una API REST propia (Spring Boot) por HTTPS.
- El servidor es un monolito modular con arquitectura hexagonal por módulo.
- PostgreSQL en el mismo VPS, sin puerto expuesto; respaldos fuera del VPS.

## Consecuencias
- Ninguna credencial de base de datos viaja en el instalador; las reglas de negocio se validan en un solo lugar.
- Hay que mantener dos aplicaciones y un contrato (módulo `contracts`).
- Sin internet la tienda no opera (decisión explícita; mitigación: router con respaldo 4G).
- Microservicios descartados: costo operativo sin beneficio para una tienda.
