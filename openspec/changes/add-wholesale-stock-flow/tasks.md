# Tasks: Add Wholesale vs. Stock Order Flow

Status legend: `[ ]` pending · `[x]` done · `[~]` in progress

## Phase F1 — Backend models & schema (✅ done)

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

## Phase F2 — DeliveryScheduleService

- [ ] Service with `getNextDeliveryDates(now, n)`, `getCutoffFor(deliveryDate)`,
      `isWithinWindow(deliveryDate, now)` using the AR timezone clock.
- [ ] Unit tests covering: same-day-before-cutoff, same-day-after-cutoff,
      weekend edges, disabled window, multi-week look-ahead.

## Phase F3 — OrderService refactor

- [ ] Split `create` into `createWholesale` (no stock check, requires date) and
      `createStock` (current behavior).
- [ ] `updateMine` branches by `type`.
- [ ] `ensureEditable` adds cutoff check for WHOLESALE.
- [ ] `transitionStatus` only restores stock on STOCK orders.
- [ ] Unit tests for both flows + edge cases.

## Phase F4 — Controllers & endpoints

- [ ] `POST /api/orders/wholesale` (`ROLE_CUSTOMER`).
- [ ] `POST /api/orders/stock` (`ROLE_CUSTOMER`).
- [ ] `GET/POST/PUT/DELETE /api/admin/delivery-windows`.
- [ ] `GET /api/config/delivery-windows` (público).
- [ ] `?type=WHOLESALE|STOCK` filter in admin/distributor order listings.
- [ ] Swagger annotations updated.

## Phase F5 — Frontend: catalog majorista

- [ ] Drop stock capping in `CatalogoComponent` / `cart.store`.
- [ ] Drop `cart/check-stock` initial fetch.

## Phase F6 — Frontend: stock-disponible

- [ ] New `StockDisponibleComponent` (filter `stock>0`, sorted desc).
- [ ] New `stockCart.store.ts` with stock capping.
- [ ] New entry in sidebar.

## Phase F7 — Frontend: confirmar refactor

- [ ] Two modes (wholesale / stock) with separate DTOs.
- [ ] Wholesale: dropdown of `getNextDeliveryDates(2)`; hide Express.
- [ ] Stock: no date; show all methods incl. Express.

## Phase F8 — Frontend: pedidos y badges

- [ ] Badge `type` in cliente mis-pedidos / detalle.
- [ ] Same badge in distribuidor pedidos + filtro.
- [ ] Distributor detail editDate from PENDIENTE for WHOLESALE.

## Phase F9 — Frontend: admin delivery-windows

- [ ] `delivery-windows.ts/html/css` — ABM con tabla, modal crear/editar.

## Phase F10 — Integration & manual QA

- [ ] `OrderServiceIntegrationTest` covering both flows end-to-end.
- [ ] Update existing tests that were broken by `minOrderAmount` (pre-existing).
- [ ] Manual smoke test.
