package com.distribuidora.deliverynote.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryNoteItemResponse(
        UUID id,
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantityDelivered
) {}
