# No aparece opción de descargar remito para rol distribuidor

**Módulo**: Backend + Frontend  
**Patrón**:  
Usuario con `ROLE_DISTRIBUTOR` accede al sistema pero no encuentra funcionalidad de remitos ni endpoint disponible.

**Diagnóstico / Fix**:  
Causa raíz distribuida:
1. Backend: solo existía `AdminDeliveryNoteController` restringido a `ROLE_ADMIN`. Se agregó `DistributorDeliveryNoteController` con base `/api/distributor/delivery-notes` y acceso para `ROLE_DISTRIBUTOR`.
2. Frontend: el servicio `delivery-note.service.ts` estaba hardcodeado a `/api/admin/delivery-notes`, el navbar no incluía el ítem `Remitos` para distribuidor, y las rutas internas estaban hardcodeadas como `/admin/remitos/...` en lugar de relativas.
3. Backend: el remito no se generaba automáticamente al cambiar el pedido a `ARMADO`. Ahora `OrderService.transitionInternal` genera el remito automáticamente para pedidos `WHOLESALE` al pasar a `ARMADO`. La validación de `DeliveryNoteService.generateFromOrder` solo permite `ARMADO` (no `ENVIADO`).
4. Frontend: el detalle de pedido no mostraba los remitos asociados ni recargaba la lista después de cambiar estado. Ahora carga y muestra la sección `Remitos` con botón de generar si falta.

**Ver también (Frontend)**: [`missing-remitos-frontend.md`](../distribuidora-ui/docs/errors/missing-remitos-frontend.md)
