package com.distribuidora.dto.delivery;

import com.distribuidora.model.DeliveryMethodScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload to replace a {@link com.distribuidora.model.DeliveryMethod}.
 */
public record UpdateDeliveryMethodRequest(

    @Schema(description = "Nombre único del método de entrega", example = "Envío Express", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    String name,

    @Schema(description = "Costo del envío en pesos", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 7, fraction = 2)
    BigDecimal cost,

    @Schema(description = "Días hábiles estimados para la entrega", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Min(0)
    Integer estimatedDays,

    @Schema(description = "A qué flujos aplica el método.",
            example = "BOTH",
            allowableValues = {"WHOLESALE", "STOCK", "BOTH"})
    DeliveryMethodScope appliesToOrderType
) {}
