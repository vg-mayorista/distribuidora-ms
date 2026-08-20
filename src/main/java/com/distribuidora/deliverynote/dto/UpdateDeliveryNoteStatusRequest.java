package com.distribuidora.dto.deliverynote;

import com.distribuidora.deliverynote.model.DeliveryNoteStatus;

public record UpdateDeliveryNoteStatusRequest(
        DeliveryNoteStatus targetStatus,
        String notes
) {}
