package com.distribuidora.dto.config;

import com.distribuidora.dto.delivery.DeliveryWindowResponse;

import java.time.Instant;
import java.util.List;

public record BusinessConfigResponse(
        Integer minPacksPerLine,
        List<DeliveryWindowResponse> deliveryWindows,
        Instant updatedAt
) {}
