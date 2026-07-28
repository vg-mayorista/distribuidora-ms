package com.distribuidora.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload to create a {@link com.distribuidora.model.Category}.
 */
public record CreateCategoryRequest(

        @Schema(description = "Nombre único de la categoría (case-sensitive)", example = "Verduras", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String name
) {
}
