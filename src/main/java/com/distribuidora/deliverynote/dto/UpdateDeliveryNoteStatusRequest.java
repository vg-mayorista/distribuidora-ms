package com.distribuidora.deliverynote.dto;

import com.distribuidora.deliverynote.model.DeliveryNoteStatus;

public record UpdateDeliveryNoteStatusRequest(
        DeliveryNoteStatus targetStatus,
        String notes
) {}
