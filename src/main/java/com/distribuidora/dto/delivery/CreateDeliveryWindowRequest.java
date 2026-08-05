package com.distribuidora.dto.delivery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record CreateDeliveryWindowRequest(
        @NotNull
        @Min(1)
        @Max(7)
        Integer cutoffDayOfWeek,

        @NotNull
        LocalTime cutoffTime,

        @NotNull
        @Min(1)
        @Max(7)
        Integer deliveryDayOfWeek,

        @Size(max = 100)
        String description,

        Boolean active
) {}
