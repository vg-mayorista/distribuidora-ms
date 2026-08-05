package com.distribuidora.dto.delivery;

import com.distribuidora.model.DeliveryWindow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record DeliveryWindowResponse(
        @Schema(description = "ID de la ventana", example = "a3f2b8c1-7d4e-4f1a-b5c3-9e8d2f6a1b4c")
        UUID id,

        @Schema(description = "Día de la semana del corte (1=lunes..7=domingo, ISO)", example = "2")
        Integer cutoffDayOfWeek,

        @Schema(description = "Hora del corte en zona horaria local del negocio", example = "18:00:00")
        LocalTime cutoffTime,

        @Schema(description = "Día de la semana de la entrega (1=lunes..7=domingo, ISO)", example = "3")
        Integer deliveryDayOfWeek,

        @Schema(description = "Descripción legible", example = "Pedidos hasta Mar 18 h → entrega Mié")
        String description,

        @Schema(description = "Si la ventana está activa y se considera en los cálculos", example = "true")
        Boolean active,

        @Schema(description = "Fecha y hora de creación", example = "2026-06-17T15:00:00Z")
        Instant createdAt,

        @Schema(description = "Fecha y hora de última actualización", example = "2026-06-17T15:00:00Z")
        Instant updatedAt
) {
    public static DeliveryWindowResponse from(DeliveryWindow w) {
        return new DeliveryWindowResponse(
                w.getId(),
                w.getCutoffDayOfWeek(),
                w.getCutoffTime(),
                w.getDeliveryDayOfWeek(),
                w.getDescription(),
                w.getActive(),
                w.getCreatedAt(),
                w.getUpdatedAt()
        );
    }
}
