package com.distribuidora.dto.order;

import com.distribuidora.model.OrderStatus;
import com.distribuidora.model.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        String customerName,
        String customerEmail,
        OrderStatus status,
        OrderType type,
        UUID deliveryMethodId,
        String deliveryMethodName,
        BigDecimal deliveryCost,
        BigDecimal subtotal,
        BigDecimal total,
        String deliveryAddress,
        String deliveryPhone,
        String notes,
        com.distribuidora.model.PaymentMethod paymentMethod,
        String paymentReceiptUrl,
        LocalDate deliveryDate,
        Boolean editable,
        Integer itemCount,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {}
