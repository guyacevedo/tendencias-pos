# F5 — Producción

## 5.1 Instalador y actualización
`jpackage` (MSI Windows; DMG opcional) con runtime recortado por `jlink`; icono y nombre; configuración
de la URL de la API en `%APPDATA%`. Al iniciar, comparar versión con `/api/version`: si es menor que
`minClientVersion`, bloquear y ofrecer descargar desde GitHub Releases.
Workflow `release-desktop.yml` en etiquetas `v*` (runner windows-latest).
**Aceptación:** MSI instalable en un Windows limpio.

## 5.2 Despliegue, respaldos y monitoreo
Workflow de despliegue probado de punta a punta; `backup` en compose (pg_dump diario cifrado con `age`
o `gpg`, subida a un bucket externo, retención 7 diarios + 4 semanales); `docs/restore.md` y una
restauración probada; monitoreo externo de `/actuator/health`; rotación de logs; endurecimiento del VPS
documentado (SSH con llave, firewall, actualizaciones).
**Aceptación:** API en producción con HTTPS, respaldo restaurado en local.

## 5.3 Revisión de seguridad
Revisión independiente (subagente sin contexto previo) contra OWASP ASVS nivel 1: autenticación,
autorización por endpoint, validación, secretos, dependencias (Trivy + Dependabot), cabeceras.
Corregir hallazgos altos y críticos. **Aceptación:** informe en `docs/security-review.md` sin
pendientes críticos.

## 5.4 Piloto
Checklist de instalación en la tienda, usuario admin real, cajas, rangos de numeración, capacitación
corta (`docs/manual.md`). **Aceptación:** una semana de uso real sin incidentes bloqueantes.
