package com.distribuidora.dto.config;

import com.distribuidora.dto.delivery.DeliveryWindowResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BusinessConfigResponse(
        Integer minPacksPerLine,
        BigDecimal minOrderAmount,
        List<DeliveryWindowResponse> deliveryWindows,
        Instant nextCutoffInstant,
        Instant updatedAt
) {}
