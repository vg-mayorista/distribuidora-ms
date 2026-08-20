package com.distribuidora.dto.deliverynote;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryNoteItemResponse(
        UUID id,
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantityDelivered
) {}
