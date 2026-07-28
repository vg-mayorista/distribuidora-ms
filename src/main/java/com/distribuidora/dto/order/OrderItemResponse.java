package com.distribuidora.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String productImageUrl,
        Integer quantity,
        Integer packsRequested,
        Integer unitsPerPackAtOrder,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
