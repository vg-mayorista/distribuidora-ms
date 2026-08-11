package com.distribuidora.dto.delivery;

import com.distribuidora.model.DeliveryMethod;
import com.distribuidora.model.DeliveryMethodScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-side projection of a {@link com.distribuidora.model.DeliveryMethod}.
 */
public record DeliveryMethodResponse(

    @Schema(description = "ID único del método de entrega", example = "a3f2b8c1-7d4e-4f1a-b5c3-9e8d2f6a1b4c")
    UUID id,

    @Schema(description = "Nombre del método de entrega", example = "Envío Express")
    String name,

    @Schema(description = "Costo del envío en pesos", example = "150.00")
    BigDecimal cost,

    @Schema(description = "Días hábiles estimados para la entrega", example = "3")
    Integer estimatedDays,

    @Schema(description = "Indica si el método está activo", example = "true")
    Boolean active,

    @Schema(description = "Para qué tipo de pedido aplica el método", example = "BOTH")
    DeliveryMethodScope appliesToOrderType,

    @Schema(description = "Fecha y hora de creación", example = "2026-06-17T15:00:00Z")
    Instant createdAt,

    @Schema(description = "Fecha y hora de última actualización", example = "2026-06-17T15:00:00Z")
    Instant updatedAt
) {
    public static DeliveryMethodResponse from(DeliveryMethod dm) {
        return new DeliveryMethodResponse(
            dm.getId(),
            dm.getName(),
            dm.getCost(),
            dm.getEstimatedDays(),
            dm.getActive(),
            // Defensive: si el valor quedó NULL en la DB por una migración
            // incompleta, lo normalizamos a BOTH para que el response nunca
            // devuelva null en un campo no-nullable.
            dm.getAppliesToOrderType() != null ? dm.getAppliesToOrderType() : DeliveryMethodScope.BOTH,
            dm.getCreatedAt(),
            dm.getUpdatedAt()
        );
    }
}
