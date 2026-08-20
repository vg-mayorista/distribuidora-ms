package com.distribuidora.dto.deliverynote;

import com.distribuidora.deliverynote.model.DeliveryNoteStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DeliveryNoteResponse(
        UUID id,
        UUID orderId,
        String deliveryNoteNumber,
        DeliveryNoteStatus status,
        LocalDate issueDate,
        LocalDate deliveryDate,
        String notes,
        List<DeliveryNoteItemResponse> items,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {}
