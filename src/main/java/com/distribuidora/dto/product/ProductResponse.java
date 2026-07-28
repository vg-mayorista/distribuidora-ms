package com.distribuidora.dto.product;

import com.distribuidora.model.Product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    @Schema(description = "ID único del producto (UUID v4)")
    UUID id,

    @Schema(description = "ID de la categoría del producto")
    UUID categoryId,

    @Schema(description = "Nombre del producto")
    String name,

    @Schema(description = "Descripción detallada")
    String description,

    @Schema(description = "Precio unitario por pack/caja")
    BigDecimal price,

    @Schema(description = "Stock disponible en unidades físicas")
    Integer stock,

    @Schema(description = "Unidades físicas que contiene cada pack/caja")
    Integer unitsPerPack,

    @Schema(description = "URL de la imagen del producto")
    String imageUrl,

    @Schema(description = "Estado de actividad del producto (false implica baja lógica/soft delete)")
    Boolean active,

    @Schema(description = "Fecha y hora de creación")
    Instant createdAt,

    @Schema(description = "Fecha y hora de última actualización")
    Instant updatedAt
) {
  public static ProductResponse from(Product p) {
    return new ProductResponse(
        p.getId(),
        p.getCategoryId(),
        p.getName(),
        p.getDescription(),
        p.getPrice(),
        p.getStock(),
        p.getUnitsPerPack(),
        p.getImageUrl(),
        p.getActive(),
        p.getCreatedAt(),
        p.getUpdatedAt());
  }
}
