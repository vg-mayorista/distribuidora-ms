package com.distribuidora.dto.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBusinessConfigRequest(
        @NotNull(message = "El monto mínimo no puede ser nulo")
        @DecimalMin(value = "0.0", message = "El monto mínimo debe ser mayor o igual a 0")
        BigDecimal minOrderAmount,

        @NotNull(message = "El mínimo de unidades no puede ser nulo")
        @Min(value = 0, message = "El mínimo de unidades debe ser mayor o igual a 0")
        Integer minOrderUnits
) {}
