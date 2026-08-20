# AGENTS.md — Delivery Notes (Remitos)

## Contexto

Esta es la **funcionalidad de Remitos** dentro del microservicio `distribuidora-ms`. Un **Remito** es el documento de entrega que genera el distribuidor cuando prepara y despacha un pedido. Representa la mercadería efectivamente entregada y sirve como constancia física/legal de la operación.

Los remitos se generan sobre pedidos existentes (principalmente en estados `ARMADO` o `ENVIADO`) y reflejan las cantidades realmente entregadas, que pueden diferir de lo pedido (faltantes, roturas, reemplazos).

**Template físico**: existe un template base en `src/main/java/com/distribuidora/deliverynote/docs/V&G - Remito.docx` que debe usarse como base para generar el documento de remito.

## Template y campos

El template `V&G - Remito.docx` contiene los siguientes campos que deben completarse con datos del pedido:

| Campo template | Origen de dato | Descripción |
|----------------|----------------|-------------|
| Nombre / Razón Social | `user.firstName + " " + user.lastName` (o campo futuro `businessName`) | Cliente que recibe |
| Zona de Entrega | `user.zone` | Zona del cliente |
| Fecha | `order.deliveryDate` (si existe) o fecha actual | Fecha de entrega programada o de emisión |
| Remito Nº | `delivery_note_number` generado | Número secuencial único |
| Tabla productos | `order.items` | Cantidad, producto, valor unitario, total por línea |
| TOTAL | Suma de subtotales del pedido | Importe total |
| Observaciones | `delivery_note.notes` o texto ingresado por admin | Notas adicionales |
| Firma y Aclaración | — (campo vacío para firmar a mano) | No se llena digitalmente |
| Forma de pago | — | EFECTIVO / TRANSFERENCIA (seleccionar al generar) |

## Reglas de negocio (resumen)

1. **Scope**: Por el momento, solo se genera remito para pedidos **mayoristas** (`OrderType.WHOLESALE`).
2. **Generación**: Un remito se genera desde una Order existente en estados `ARMADO` o `ENVIADO`.
3. **Numeración**: El número de remito es único y secuencial por período (ej: `R-2026-0001`). Se genera en el Service al crear el remito. Si se cancela en `PENDING`, se libera el número.
4. **Uno por orden**: Una orden tiene un único remito.
5. **Items**: El remito contiene ítems que copian datos de la Order (`productId`, `productName`, `unitPrice`), más `quantityDelivered` (igual a la cantidad pedida en esta fase).
6. **Template**: Se genera usando el template `V&G - Remito.docx` con **Apache POI** on-demand al descargar.
7. **Corte**: Se puede descargar el remito on-demand una vez pasada la hora de corte (Martes y Jueves 18:00).
8. **Transiciones de estado**:
   - `PENDING` → `GENERATED` (al crear)
   - `GENERATED` → `DELIVERED` (al marcar entregado físicamente)
   - Cualquier estado → `CANCELED` (solo si no fue entregado)
9. **Inmutabilidad**: Una vez `DELIVERED`, el remito no se puede modificar ni cancelar.
10. **Evento**: Al crear el remito, se dispara `DeliveryNoteCreatedEvent` para notificar al cliente.

## Inconsistencias / Dudas (revisar más adelante)

- [`docs/doubts.md`](../../../../../docs/doubts.md): forma de pago (por ahora fuera de alcance).
- [`docs/doubts.md`](../../../../../docs/doubts.md): modificar registro de usuario para incluir `businessName` (razón social).
- Numeración: confirmar si liberar números cancelados en `PENDING`.

## Notas importantes

- **No** exponer este recurso a clientes en la Fase 1. Solo admin/distribuidor.
- **No** modificar el stock desde el remito. El stock ya fue descontado al confirmar la orden.
- **No** generar números de remito hardcodeados. Usar generación atómica en Java.
- **No** hacer `push` a `main` directamente durante la creación de este módulo. Usar feature branches.
- **Sí** reutilizar lógica existente de `OrderService` para validar que la orden existe y está en estado permitido.
- **Sí** usar el mismo `Clock` de Argentina que el resto del proyecto (`ZoneId.of("America/Argentina/Buenos_Aires")`).
- **Sí** disparar eventos de dominio para integración futura con notificaciones.
- **Sí** usar el template físico `V&G - Remito.docx` como base para la generación del documento.
