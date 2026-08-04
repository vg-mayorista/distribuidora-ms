package com.distribuidora.dto.config;

import java.math.BigDecimal;
import java.time.Instant;

public record BusinessConfigResponse(
        BigDecimal minOrderAmount,
        Integer minOrderUnits,
        Instant updatedAt
) {}
