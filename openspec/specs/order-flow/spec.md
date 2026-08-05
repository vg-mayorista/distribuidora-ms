# Spec — Order Flow

## Purpose

Definir los dos flujos de pedido que soporta el sistema, sus diferencias operativas y cómo se
exponen al cliente y al distribuidor. Define los invariantes que el código debe respetar.

## Requirements

### R1. Tipo de pedido (`Order.type`)

Todo pedido pertenece exactamente a uno de dos tipos:

- **WHOLESALE** — pedido al catálogo general. La mercadería aún no existe en depósito.
  - No consulta ni descuenta `Product.stock`.
  - `deliveryDate` obligatorio y debe estar en las próximas fechas válidas.
  - Solo permite métodos de entrega con `appliesToOrderType` ∈ `{WHOLESALE, BOTH}`.

- **STOCK** — pedido contra el excedente físico ya en depósito.
  - Descuenta `Product.stock` al confirmar y lo restaura al cancelar / transición a CANCELADO.
  - `deliveryDate` se ignora (queda `null`).
  - Permite cualquier método de entrega con `appliesToOrderType` ∈ `{STOCK, BOTH}`.

### R2. Ventanas de entrega (`DeliveryWindow`)

Una ventana semanal es `(cutoffDayOfWeek, cutoffTime, deliveryDayOfWeek, active)`.

- `cutoffDayOfWeek` y `deliveryDayOfWeek` ∈ `[1..7]` (ISO DayOfWeek).
- Solo ventanas `active=true` se usan para calcular fechas disponibles.
- Una fecha de entrega es elegible mientras `now < cutoffFor(deliveryDate)`.
- Seed inicial: mar 18:00 → mié, jue 18:00 → vie (ambas activas).

### R3. Editabilidad de pedidos WHOLESALE (`ensureEditable`)

Un pedido WHOLESALE es editable si y solo si:

- `status == PENDIENTE`, **y**
- `now < getCutoffFor(deliveryDate)` (la hora actual está estrictamente antes del corte).

Pasado el corte, el pedido se considera "bloqueado" y el distribuidor debe proceder a armar.

### R4. Visibilidad de stock

- El **catálogo mayorista** (`/cliente/catalogo`) lista todos los productos activos, sin
  información de stock. Los inputs de cantidad no están capeados.
- El **catálogo de stock** (`/cliente/stock-disponible`) lista solo productos con
  `Product.stock > 0`, ordenados por cantidad disponible desc. Los inputs sí están capeados.
- El admin gestiona el stock vía `admin/stock` (etiquetado "Excedente").

### R5. Métodos de entrega por scope (`DeliveryMethod.appliesToOrderType`)

- `BOTH`: visible en ambos flujos (default).
- `STOCK`: solo en pedidos STOCK (ej. "Envío Express").
- `WHOLESALE`: solo en pedidos mayoristas.

### R6. Compatibilidad hacia atrás

- Pedidos preexistentes en BD quedan con `type='STOCK'` (default de la columna).
- `DeliveryMethod.appliesToOrderType='BOTH'` por default; el script de migración corrige
  "Envío Express" a `STOCK` si quedó mal seteado.
