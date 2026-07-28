package com.distribuidora.dto.category;

import com.distribuidora.model.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-side projection of a {@link com.distribuidora.model.Category}.
 */
public record CategoryResponse(
        @Schema(description = "ID único de la categoría", example = "a3f2b8c1-7d4e-4f1a-b5c3-9e8d2f6a1b4c")
        UUID id,

        @Schema(description = "Nombre de la categoría (case-sensitive)", example = "Verduras")
        String name,

        @Schema(description = "Estado de actividad de la categoría (false implica baja lógica/soft delete)", example = "true")
        Boolean active,

        @Schema(description = "Fecha y hora de creación", example = "2026-06-17T15:00:00Z")
        Instant createdAt,

        @Schema(description = "Fecha y hora de última actualización", example = "2026-06-17T15:00:00Z")
        Instant updatedAt) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getActive(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
