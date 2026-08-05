package com.distribuidora.dto.delivery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record UpdateDeliveryWindowRequest(
        @Min(1)
        @Max(7)
        Integer cutoffDayOfWeek,

        LocalTime cutoffTime,

        @Min(1)
        @Max(7)
        Integer deliveryDayOfWeek,

        @Size(max = 100)
        String description,

        Boolean active
) {}
