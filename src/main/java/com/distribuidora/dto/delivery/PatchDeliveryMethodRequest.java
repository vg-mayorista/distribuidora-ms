package com.distribuidora.dto.delivery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload to partially update a {@link com.distribuidora.model.DeliveryMethod}.
 *
 * <p>All fields are nullable. Null means "do not update".
 */
public record PatchDeliveryMethodRequest(

    @Schema(description = "Nombre del método de entrega", example = "Envío Express")
    @Size(max = 100)
    String name,

    @Schema(description = "Costo del envío en pesos", example = "150.00")
    @DecimalMin("0.00")
    @Digits(integer = 7, fraction = 2)
    BigDecimal cost,

    @Schema(description = "Días hábiles estimados para la entrega", example = "3")
    @Min(0)
    Integer estimatedDays
) {}
