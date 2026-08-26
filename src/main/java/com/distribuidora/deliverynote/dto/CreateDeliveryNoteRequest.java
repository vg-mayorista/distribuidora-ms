package com.distribuidora.deliverynote.dto;

import java.util.List;
import java.util.UUID;

public record CreateDeliveryNoteRequest(
        UUID orderId,
        List<DeliveryNoteItemRequest> items,
        String notes
) {
    public record DeliveryNoteItemRequest(
            UUID productId,
            String productName,
            java.math.BigDecimal unitPrice,
            Integer quantityDelivered
    ) {}
}
