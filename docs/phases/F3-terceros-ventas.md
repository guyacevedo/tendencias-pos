# F3 — Terceros y ventas

## 3.1 Clientes y proveedores
Generar con `scripts/new-module parties Party`. Campos: tipo (cliente/proveedor/ambos), tipo de
documento (CC, NIT, CE, PP), número (único por tipo), nombre, teléfono, correo, dirección.
"Cliente general" sembrado por migración. **Aceptación:** CRUD y búsqueda rápida por nombre/documento.

## 3.2 Venta en el servidor
**Hacer**
- `numbering_range` (prefijo, desde, hasta, actual, tipo de documento) con asignación transaccional
  del consecutivo.
- `sales_document` + `sales_line` (variante, cantidad, precio, descuento, IVA, total) + `payment`.
- `POST /api/sales` con cabecera `Idempotency-Key` (tabla `idempotency_key`: misma llave → misma
  respuesta, sin efectos repetidos). En UNA transacción: valida, descuenta stock vía `StockLedger`,
  asigna consecutivo, registra pago y movimiento de caja (si hay sesión de caja: desde 4.2).
- Venta a crédito: saldo pendiente; `POST /api/sales/{id}/payments` para abonos (no supera el saldo).
- `POST /api/sales/{id}/void` (ADMIN, motivo): devuelve stock y revierte caja con tipo propio.
- Totales calculados SIEMPRE en el servidor; el cliente solo envía variantes, cantidades y descuentos.
- Definir el puerto `billing.ElectronicInvoiceProvider` (sin implementación; la DIAN llega en F7).
**Aceptación:** pruebas de venta feliz, sin stock, reenvío con la misma llave, abono mayor al saldo,
anular dos veces.

## 3.3 Pantalla de caja
**Hacer**
- Campo de escaneo con foco permanente (lector = teclado + Enter), búsqueda F2, cantidad, quitar línea,
  descuento, cliente (por defecto "Cliente general"), cobrar F4 (efectivo, transferencia, crédito),
  cambio a devolver, Esc cancela. Totales grandes.
- Lista de ventas con filtros, detalle, abonos y anulación (según rol).
**Aceptación:** venta completa solo con teclado; presentador con pruebas.

## 3.4 Facturas impresas
**Hacer**
- Plantillas JasperReports 7 precompiladas en el build: carta y tirilla 80 mm; datos de la tienda,
  consecutivo, líneas, totales, pagos, saldo.
- Impresión directa a la impresora elegida y vista previa; formato según `store_settings`.
**Aceptación:** PDF de ejemplo de ambos formatos generado en prueba; impresión de prueba por el usuario.
