package com.distribuidora.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload to fully replace a {@link com.distribuidora.model.Category}.
 *
 * <p>This is a <b>full update</b> (PUT semantics): every field is required. For
 * partial updates use PATCH. Note that {@code active} is not part of this DTO
 * on purpose &mdash; deactivation/reactivation has its own dedicated endpoint.
 */
public record UpdateCategoryRequest(

        @Schema(description = "Nuevo nombre de la categoría (case-sensitive)", example = "Frutas", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String name
) {
}
