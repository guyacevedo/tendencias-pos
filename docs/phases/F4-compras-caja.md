# F4 — Compras y caja

## 4.1 Compras
Reutilizar el motor de documentos de ventas (líneas, pagos, anulación) con signo contrario en stock y
caja. Número de factura del proveedor (texto) + consecutivo interno. Actualiza precio de compra del
producto opcionalmente. **Aceptación:** compra suma stock y deja kardex; anularla con stock ya vendido
devuelve `STOCK_INSUFICIENTE`.

## 4.2 Caja
**Hacer**
- `cash_register` (cajas físicas), `cash_session` (usuario, apertura con base, cierre con conteo,
  diferencia), `cash_movement` (tipo: VENTA, ABONO_VENTA, COMPRA, ABONO_COMPRA, GASTO, INGRESO, RETIRO,
  ANULACION_VENTA, ANULACION_COMPRA; monto; medio de pago; documento).
- Sin sesión abierta no se puede vender ni pagar en efectivo (conectar con 3.2 y 4.1).
- Movimientos manuales con motivo; arqueo al cierre (esperado vs contado) y reporte de cierre imprimible.
**Aceptación:** día completo de prueba: abrir, comprar, vender, gasto, cerrar con diferencia 0.
