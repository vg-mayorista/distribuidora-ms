package com.distribuidora.dto.order;

import com.distribuidora.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull
        OrderStatus targetStatus,
        String notes
) {}
