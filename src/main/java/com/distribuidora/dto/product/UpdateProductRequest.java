package com.distribuidora.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(

        @Schema(description = "ID de la categoría a la que pertenece el producto", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID categoryId,

        @Schema(description = "URL de la imagen del producto", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String imageUrl,

        @Schema(description = "Nombre único del producto", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "Descripción detallada del producto", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 500)
        String description,

        @Schema(description = "Precio unitario por pack/caja (debe ser >= 0.00)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 7, fraction = 2)
        BigDecimal price,

        @Schema(description = "Stock disponible en unidades físicas", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(0)
        Integer stock,

        @Schema(description = "Unidades físicas que contiene cada pack/caja (default 1 = unitario)", example = "12", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Min(1)
        Integer unitsPerPack,

        @Schema(description = "Umbral de unidades físicas por debajo del cual el producto se considera 'Stock bajo'", example = "20", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Min(0)
        Integer lowStockThreshold
) {
}
