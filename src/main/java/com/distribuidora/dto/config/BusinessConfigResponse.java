package com.distribuidora.dto.config;

import com.distribuidora.dto.delivery.DeliveryWindowResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BusinessConfigResponse(
        BigDecimal minOrderAmount,
        Integer minOrderUnits,
        List<DeliveryWindowResponse> deliveryWindows,
        Instant updatedAt
) {}
