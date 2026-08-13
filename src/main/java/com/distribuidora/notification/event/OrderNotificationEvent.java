package com.distribuidora.notification.event;

import com.distribuidora.model.OrderType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class OrderNotificationEvent {

    private final UUID orderId;
    private final OrderType orderType;
    private final String customerName;
    private final String customerPhone;
    private final String deliveryAddress;
    private final BigDecimal subtotal;
    private final BigDecimal total;
    private final int itemsCount;
    private final Instant createdAt;
}
