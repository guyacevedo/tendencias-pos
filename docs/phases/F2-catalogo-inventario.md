# F2 — Catálogo e inventario

## 2.1 Módulo de referencia: Categorías + plantilla
Este módulo es el patrón que se copia después; hacerlo impecable.
**Hacer**
- Servidor `catalog/category`: dominio (`Category` con invariantes), caso de uso, puerto de persistencia,
  adaptador JPA, controlador `GET (paginado, búsqueda) / POST / PUT / PATCH desactivar`, validación,
  Flyway, pruebas (dominio, `@WebMvcTest`, integración `@Tag("it")`).
- Escritorio: `CrudListView` + `CrudFormView` genéricos en `ui-kit` (tabla con búsqueda y paginación,
  formulario en MigLayout de 2→1 columnas), presentador de Categorías con pruebas.
- `scripts/new-module <modulo> <Entidad>`: genera el esqueleto copiando Categorías con sed (servidor +
  escritorio + migración + pruebas). Documentar uso en CLAUDE.md.
**Aceptación:** CRUD de categorías completo de punta a punta; `scripts/new-module catalog Brand` genera
código que compila (luego se borra).

## 2.2 Productos y variantes
**Hacer**
- `product` (nombre, categoría, descripción, precio compra, precio venta, IVA, activo) y
  `product_variant` (talla, color opcional, código de barras único, stock mínimo, activa).
- Alta de producto con su rejilla de tallas en una sola operación; generación de código de barras
  (EAN-13 interno con prefijo 2xx o Code 128) y validación de unicidad.
- Búsqueda por nombre, código de barras o categoría; endpoint de búsqueda rápida para la caja.
- Etiquetas: PDF con ZXing (N etiquetas por variante), generado en el escritorio.
**Aceptación:** crear un zapato con 6 tallas, buscarlo por código leído y generar su PDF de etiquetas.

## 2.3 Stock y kardex
**Hacer**
- `stock` (variant_id PK, quantity ≥ 0 con CHECK, version) y `stock_movement` (tipo, cantidad con signo,
  saldo resultante, documento de origen, usuario, fecha).
- Servicio de dominio `StockLedger.apply(movements)` usado por ventas, compras y ajustes; bloqueo
  pesimista `SELECT … FOR UPDATE` ordenado por id para evitar interbloqueos.
- Ajustes manuales (ADMIN) con motivo obligatorio; consulta de kardex por variante con filtros de fecha.
- Pantalla de inventario: existencias por producto/talla, resaltado bajo mínimo, kardex.
**Aceptación:** prueba de concurrencia (dos hilos vendiendo la última unidad: uno gana, el otro recibe
`STOCK_INSUFICIENTE`); un ajuste deja movimiento auditable.
