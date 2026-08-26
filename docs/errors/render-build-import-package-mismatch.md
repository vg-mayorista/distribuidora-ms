# Fallo en el Build de Docker/Render por paquete desalineado en controladores

**Módulo**: Backend (`deliverynote`)  
**Patrón**:  
```text
[ERROR] AdminDeliveryNoteController.java:[79,33] cannot find symbol class UpdateDeliveryNoteStatusRequest
[ERROR] DistributorDeliveryNoteController.java:[3,42] package com.distribuidora.dto.deliverynote does not exist
```

**Diagnóstico / Fix**:  
- **Causa raíz**: Al refactorizar las clases DTO al paquete `com.distribuidora.deliverynote.dto`, los controladores `AdminDeliveryNoteController` y `DistributorDeliveryNoteController` mantuvieron importaciones del paquete anterior `com.distribuidora.dto.deliverynote.*`, además de una declaración de paquete desalineada en `AdminDeliveryNoteController`.
- **Solución**: 
  1. Se actualizó la declaración de paquete en `AdminDeliveryNoteController.java` a `package com.distribuidora.deliverynote.controller;`.
  2. Se actualizaron los imports en ambos controladores a `com.distribuidora.deliverynote.dto.*`.
