package com.distribuidora.mapper;

import com.distribuidora.model.Product;
import com.distribuidora.dto.product.CreateProductRequest;
import com.distribuidora.dto.product.PatchProductRequest;
import com.distribuidora.dto.product.UpdateProductRequest;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

  public Product toEntity(CreateProductRequest req) {
    return Product.builder()
        .categoryId(req.categoryId())
        .imageUrl(req.imageUrl())
        .name(req.name())
        .description(req.description())
        .price(req.price())
        .stock(req.stock() != null ? req.stock() : 0)
        .unitsPerPack(req.unitsPerPack() != null ? req.unitsPerPack() : 1)
        .lowStockThreshold(req.lowStockThreshold())
        .active(Boolean.TRUE)
        .build();
  }

  public void applyUpdate(Product target, UpdateProductRequest req) {
    target.setCategoryId(req.categoryId());
    target.setImageUrl(req.imageUrl());
    target.setName(req.name());
    target.setDescription(req.description());
    target.setPrice(req.price());
    target.setStock(req.stock());
    target.setUnitsPerPack(req.unitsPerPack() != null ? req.unitsPerPack() : 1);
    target.setLowStockThreshold(req.lowStockThreshold());
  }

  public void applyPatch(Product target, PatchProductRequest req) {
    if (req.categoryId() != null) {
      target.setCategoryId(req.categoryId());
    }
    if (req.imageUrl() != null) {
      target.setImageUrl(req.imageUrl());
    }
    if (req.name() != null) {
      target.setName(req.name());
    }
    if (req.description() != null) {
      target.setDescription(req.description());
    }
    if (req.price() != null) {
      target.setPrice(req.price());
    }
    if (req.stock() != null) {
      target.setStock(req.stock());
    }
    if (req.unitsPerPack() != null) {
      target.setUnitsPerPack(req.unitsPerPack());
    }
    if (req.lowStockThreshold() != null) {
      target.setLowStockThreshold(req.lowStockThreshold());
    }
  }
}
