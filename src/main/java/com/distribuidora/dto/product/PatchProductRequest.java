package com.distribuidora.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

public record PatchProductRequest(
        @Schema(description = "ID de la categoría a asignar (null = no cambia)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID categoryId,

        @Schema(description = "URL de la imagen del producto (null = no cambia)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String imageUrl,

        @Schema(description = "Nuevo nombre del producto (null = no cambia)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,

        @Schema(description = "Nueva descripción del producto (null = no cambia)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        @Schema(description = "Nuevo precio del producto (null = no cambia, debe ser >= 0.00)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal price,

        @Schema(description = "Nuevo stock del producto (null = no cambia, debe ser >= 0)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer stock,

        @Schema(description = "Unidades físicas por pack (null = no cambia, debe ser >= 1)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer unitsPerPack,

        @Schema(description = "Nuevo umbral de stock bajo (null = no cambia, debe ser >= 0)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer lowStockThreshold
) {
}
