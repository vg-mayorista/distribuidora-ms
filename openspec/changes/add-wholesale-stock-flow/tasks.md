# Tasks: Add Wholesale vs. Stock Order Flow

Status legend: `[x]` done · `[~]` in progress

## Phase F1 — Backend models & schema (✅)

- [x] `OrderType` enum (`WHOLESALE`, `STOCK`).
- [x] `DeliveryMethodScope` enum (`WHOLESALE`, `STOCK`, `BOTH`).
- [x] `DeliveryWindow` entity + `DeliveryWindowRepository`.
- [x] `Order.type` column with default `STOCK`.
- [x] `DeliveryMethod.appliesToOrderType` column with default `BOTH`.
- [x] `DeliveryWindowResponse` / `Create/UpdateWindowRequest` DTOs.
- [x] `OrderResponse.type`, `DeliveryMethodResponse.appliesToOrderType`,
      `BusinessConfigResponse.deliveryWindows`.
- [x] `BusinessConfigService` returns windows in `getPublicConfig`.
- [x] `DatabaseSeeder` seeds two windows and updates `Envío Express` to STOCK.
- [x] Idempotent SQL migration `db/migrations/V1__add_wholesale_stock_flow.sql`.

## Phase F2 — DeliveryScheduleService (✅)

- [x] Service with `getNextDeliveryDates(n)`, `getCutoffFor(deliveryDate)`,
      `isWithinWindow(deliveryDate, now)` using the AR timezone clock.
- [x] Constructor-injectable `Clock` (con anotación `@Autowired` para el
      caso default) para que Spring elija el único ctor correcto.
- [x] Unit tests (12 casos) cubriendo: pre/post cutoff, mismo día,
      fines de semana, ventanas deshabilitadas, multi-week look-ahead.

## Phase F3 — OrderService refactor (✅)

- [x] Split `create` en `createWholesale` y `createStock`.
- [x] `updateMine` ramifica por `type`.
- [x] `ensureEditable` agrega check de cutoff para WHOLESALE.
- [x] `transitionStatus` solo maneja stock cuando `type == STOCK`.
- [x] `updateDeliveryDate` solo aplica a WHOLESALE en PENDIENTE.
- [x] Unit tests (13 casos) en `OrderServiceSplitTest`.

## Phase F4 — Controllers y endpoints (✅)

- [x] `POST /api/orders/wholesale` (`ROLE_CUSTOMER`).
- [x] `POST /api/orders/stock` (`ROLE_CUSTOMER`).
- [x] `POST /api/orders` se conserva (dispatch retrocompatible).
- [x] `GET/POST/PUT/DELETE /api/admin/delivery-windows`.
- [x] `GET /api/config/delivery-windows` (público).
- [x] `?type=WHOLESALE|STOCK` filter en admin/distributor order listings.

## Phase F5 — Frontend: catálogo mayorista (✅)

- [x] Catálogo deja de llamar `cart/check-stock`.
- [x] Badge "Sin stock" informativo (no bloquea alta).
- [x] Banner "Estás armando un pedido a fábrica…".
- [x] `cartStore` pasa a tener doble: `_wholesaleLines` + `_stockLines`
      con storage keys separadas.
- [x] Modelos nuevos: `OrderType`, `DeliveryWindow`, `DeliveryMethodScope`.

## Phase F6 — Frontend: stock-disponible (✅)

- [x] Pantalla `/cliente/stock-disponible`.
- [x] Filtra productos con `stock > 0`, ordenado por stock desc.
- [x] Banner explicativo + link al catálogo mayorista.
- [x] Carga via `cartService.checkStock` en `addToCart` (capeo por stock).

## Phase F7 — Confirmar refactor (✅)

- [x] `mode: 'wholesale' | 'stock'` por ruta y/o contenido del carrito.
- [x] Wholesale: dropdown de `getNextDeliveryDates(2)`,
      `appliesToOrderType IN (WHOLESALE, BOTH)`.
- [x] Stock: sin fecha, `appliesToOrderType IN (STOCK, BOTH)`,
      submit via `createStock`.
- [x] `mapError` traduce `DELIVERY_WINDOW_EXPIRED`.

## Phase F8 — Mis pedidos / detalle / badges (✅)

- [x] Badge `type` en `MisPedidosComponent` y `PedidoDetalleClienteComponent`.
- [x] Columna "Flujo" + chip filter `?type=` en `DistribuidorPedidosComponent`.
- [x] Badge `type` en `DistribuidorPedidoDetalleComponent` y `OrdersComponent` (admin).

## Phase F9 — Admin delivery-windows (✅)

- [x] `AdminDeliveryWindowService` (CRUD).
- [x] `AdminDeliveryWindowsComponent` (tabla + modal crear/editar + eliminar).
- [x] Ruta `/admin/delivery-windows` + entry en navbar admin.

## Phase F10 — Integration tests & QA (✅)

- [x] `OrderServiceIntegrationTest` actualizado a `createStock`/`createWholesale`
      con `BusinessConfig` de bajo umbral en `setUp()`.
- [x] `ReportServiceIntegrationTest` actualizado a `createStock` (sin
      fechas) para que el flujo minorista siga testeable.
- [x] Todos los 111 tests pasan en `mvn test`.

## Backlog (no implementado en este PR)

- Subida de comprobante de pago con cancelación automática por deadline
  (ambas flujos STOCK y WHOLESALE).
- Slots de retiro predefinidos para STOCK + Retiro Local.
- Aviso automático al cliente cuando el pedido está ARMADO.
- "Cerrar día" UI: input del excedente físico post-entrega para alimentar
  el stock.
- Implementación del modelo `Batch` / `lotesv1.1.md` (FEFO + expiración
  + recall).
- Reporte consolidado por fecha de entrega para el distribuidor (suma
  por producto de los pedidos WHOLESALE de la próxima entrega).
