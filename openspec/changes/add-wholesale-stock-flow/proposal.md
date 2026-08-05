# Cambios: Add Wholesale vs. Stock Order Flow

## Why

El modelo actual trata a todos los pedidos igual: al confirmar se descuenta stock y se exige
disponibilidad. En la práctica, la distribuidora hace pedidos semanales a fábrica y el stock
del sitio debería representar solo el **excedente** (lo que sobra después de cada reparto).

Dos problemas concretos:

1. El cliente no puede pedir a fábrica si el producto está sin stock (falso bloqueo).
2. El stock del sitio y el stock que se descuenta por pedidos mayoristas se confunden.

Adicionalmente, las entregas tienen un cronograma real (corte martes 18 h → entrega miércoles;
corte jueves 18 h → entrega viernes) que hoy no está modelado.

## What Changes

- `orders.type` (nuevo enum `WHOLESALE` | `STOCK`)
  - `WHOLESALE`: pedido al catálogo general; no toca stock; requiere fecha de entrega.
  - `STOCK`: pedido contra el excedente en depósito; descuenta/restaur a stock; sin fecha.
- `delivery_methods.applies_to_order_type` (nuevo enum `WHOLESALE` | `STOCK` | `BOTH`).
  - `Envío Express` se reclasifica a `STOCK` para no ofrecerlo en pedidos mayoristas.
- Nueva tabla `delivery_windows` configurable (ABM admin) con seed inicial de las 2 ventanas.
- Nuevo endpoint público `GET /api/config/public` devuelve también las ventanas activas.
- Nuevo `DeliveryScheduleService` (pure functions) con helpers:
  - `getNextDeliveryDates(now, n)`, `getCutoffFor(deliveryDate)`, `isWithinWindow(...)`.
- `OrderService` se refactoriza:
  - `createWholesale` (no valida stock, exige fecha válida).
  - `createStock` (comportamiento actual con stock).
  - `updateMine` respeta tipo.
  - `ensureEditable` corta por cutoff para mayorista.
- `POST /api/orders` se reemplaza por:
  - `POST /api/orders/wholesale`
  - `POST /api/orders/stock`

## Impact

- **API**: nuevo campo `type` en `OrderResponse`; nuevo campo `appliesToOrderType` en
  `DeliveryMethodResponse`; nuevo campo `deliveryWindows` en `BusinessConfigResponse`.
- **DB**: 1 ALTER TABLE en `orders`, 1 ALTER TABLE en `delivery_methods`, 1 CREATE TABLE.
- **Frontend**: nueva ruta `/cliente/stock-disponible`; catálogo mayorista deja de capeear
  por stock; confirmar detecta flujo.
- **Backlog**: subida de comprobante de pago (24 hs); slots de retiro; reporte consolidado.
