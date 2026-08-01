package com.distribuidora.dto.product;

import com.distribuidora.model.Product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Respuesta de producto. El campo `stock` sólo se incluye para usuarios con rol ADMIN o DISTRIBUTOR; para clientes y anónimos se entrega `null` junto con `stockStatus`.")
public class ProductResponse {

    @Schema(description = "ID único del producto (UUID v4)")
    private final UUID id;

    @Schema(description = "ID de la categoría del producto")
    private final UUID categoryId;

    @Schema(description = "Nombre del producto")
    private final String name;

    @Schema(description = "Descripción detallada")
    private final String description;

    @Schema(description = "Precio unitario por pack/caja")
    private final BigDecimal price;

    @Schema(description = "Stock disponible en unidades físicas. Null para ROLE_CUSTOMER y anónimos.")
    private final Integer stock;

    @Schema(description = "Unidades físicas que contiene cada pack/caja")
    private final Integer unitsPerPack;

    @Schema(description = "Estado de stock calculado. IN_STOCK si hay stock sobre el umbral, LOW_STOCK si está en o por debajo del umbral, OUT_OF_STOCK si es 0.")
    private final StockStatus stockStatus;

    @Schema(description = "Umbral de unidades por debajo del cual el producto se considera 'Stock bajo'. Null para ROLE_CUSTOMER y anónimos.")
    private final Integer lowStockThreshold;

    @Schema(description = "URL de la imagen del producto")
    private final String imageUrl;

    @Schema(description = "Estado de actividad del producto (false implica baja lógica/soft delete)")
    private final Boolean active;

    @Schema(description = "Fecha y hora de creación")
    private final Instant createdAt;

    @Schema(description = "Fecha y hora de última actualización")
    private final Instant updatedAt;

    private ProductResponse(
            UUID id,
            UUID categoryId,
            String name,
            String description,
            BigDecimal price,
            Integer stock,
            Integer unitsPerPack,
            StockStatus stockStatus,
            Integer lowStockThreshold,
            String imageUrl,
            Boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.unitsPerPack = unitsPerPack;
        this.stockStatus = stockStatus;
        this.lowStockThreshold = lowStockThreshold;
        this.imageUrl = imageUrl;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductResponse full(Product p, StockStatus status) {
        return new ProductResponse(
                p.getId(),
                p.getCategoryId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getUnitsPerPack(),
                status,
                p.getLowStockThreshold(),
                p.getImageUrl(),
                p.getActive(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    public static ProductResponse redacted(Product p, StockStatus status) {
        return new ProductResponse(
                p.getId(),
                p.getCategoryId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                null,
                p.getUnitsPerPack(),
                status,
                null,
                p.getImageUrl(),
                p.getActive(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    @com.fasterxml.jackson.annotation.JsonProperty("id")
    public UUID getId() { return id; }

    @com.fasterxml.jackson.annotation.JsonProperty("categoryId")
    public UUID getCategoryId() { return categoryId; }

    @com.fasterxml.jackson.annotation.JsonProperty("name")
    public String getName() { return name; }

    @com.fasterxml.jackson.annotation.JsonProperty("description")
    public String getDescription() { return description; }

    @com.fasterxml.jackson.annotation.JsonProperty("price")
    public BigDecimal getPrice() { return price; }

    @com.fasterxml.jackson.annotation.JsonProperty("stock")
    public Integer getStock() { return stock; }

    @com.fasterxml.jackson.annotation.JsonProperty("unitsPerPack")
    public Integer getUnitsPerPack() { return unitsPerPack; }

    @com.fasterxml.jackson.annotation.JsonProperty("stockStatus")
    public StockStatus getStockStatus() { return stockStatus; }

    @com.fasterxml.jackson.annotation.JsonProperty("lowStockThreshold")
    public Integer getLowStockThreshold() { return lowStockThreshold; }

    @com.fasterxml.jackson.annotation.JsonProperty("imageUrl")
    public String getImageUrl() { return imageUrl; }

    @com.fasterxml.jackson.annotation.JsonProperty("active")
    public Boolean getActive() { return active; }

    @com.fasterxml.jackson.annotation.JsonProperty("createdAt")
    public Instant getCreatedAt() { return createdAt; }

    @com.fasterxml.jackson.annotation.JsonProperty("updatedAt")
    public Instant getUpdatedAt() { return updatedAt; }
}
