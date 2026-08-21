# Test unitario de OrderService no compila por constructor desactualizado

**Módulo**: Backend  
**Patrón**:  
`constructor OrderService in class com.distribuidora.service.OrderService cannot be applied to given types; required: ..., com.distribuidora.deliverynote.service.DeliveryNoteService`

**Diagnóstico / Fix**:  
Se agregó `DeliveryNoteService` como dependencia en `OrderService` (para generar remito automáticamente al pasar a `ARMADO`). El test unitario `OrderServiceSplitTest.buildService()` dejó de pasarle ese parámetro. Fix: mockear `DeliveryNoteService` y agregarlo al constructor del test.

**Ver también (Frontend)**: [`../distribuidora-ui/docs/errors/missing-remitos-frontend.md`](../distribuidora-ui/docs/errors/missing-remitos-frontend.md)
