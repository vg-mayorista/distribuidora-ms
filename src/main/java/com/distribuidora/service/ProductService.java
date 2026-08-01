package com.distribuidora.service;

import com.distribuidora.config.StockProperties;
import com.distribuidora.dto.product.StockStatus;
import com.distribuidora.model.Product;
import com.distribuidora.repository.ProductRepository;
import com.distribuidora.repository.CategoryRepository;
import com.distribuidora.exception.CategoryNotFoundException;
import com.distribuidora.exception.DuplicateProductException;
import com.distribuidora.exception.ProductNotFoundException;
import com.distribuidora.dto.product.CreateProductRequest;
import com.distribuidora.dto.product.PatchProductRequest;
import com.distribuidora.dto.product.UpdateProductRequest;
import com.distribuidora.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

  private final ProductRepository repository;
  private final ProductMapper mapper;
  private final CategoryRepository categoryRepository;
  private final StockProperties stockProperties;

  public Product create(CreateProductRequest req) {
    if (req.categoryId() != null && !categoryRepository.existsByIdAndActiveTrue(req.categoryId())) {
      throw new CategoryNotFoundException(req.categoryId());
    }
    if (repository.existsByName(req.name())) {
      throw new DuplicateProductException(req.name());
    }
    Product p = mapper.toEntity(req);
    return repository.save(p);
  }

  @Transactional(readOnly = true)
  public Product getById(UUID id) {
    return repository.findByIdAndActiveTrue(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public Page<Product> list(Pageable pageable) {
    return repository.findByActiveTrue(pageable);
  }

  public Product update(UUID id, UpdateProductRequest req) {
    Product p = repository.findByIdAndActiveTrue(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
    if (req.categoryId() != null && !categoryRepository.existsByIdAndActiveTrue(req.categoryId())) {
      throw new CategoryNotFoundException(req.categoryId());
    }
    if (repository.existsByNameAndIdNot(req.name(), id)) {
      throw new DuplicateProductException(req.name());
    }
    mapper.applyUpdate(p, req);
    return p;
  }

  public void softDelete(UUID id) {
    Product p = repository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
    if (Boolean.FALSE.equals(p.getActive())) {
      return;
    }
    p.setActive(Boolean.FALSE);
  }

  public Product activate(UUID id) {
    Product p = repository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
    p.setActive(Boolean.TRUE);
    return p;
  }

  public Product patch(UUID id, PatchProductRequest req) {
    Product p = repository.findByIdAndActiveTrue(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
    if (req.categoryId() != null) {
      if (!categoryRepository.existsByIdAndActiveTrue(req.categoryId())) {
        throw new CategoryNotFoundException(req.categoryId());
      }
    }
    if (req.name() != null) {
      if (req.name().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank");
      }
      if (repository.existsByNameAndIdNot(req.name(), id)) {
        throw new DuplicateProductException(req.name());
      }
    }
    if (req.price() != null && req.price().compareTo(BigDecimal.ZERO) < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be >= 0");
    }
    if (req.stock() != null && req.stock() < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stock must be >= 0");
    }
    if (req.unitsPerPack() != null && req.unitsPerPack() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unitsPerPack must be >= 1");
    }
    if (req.lowStockThreshold() != null && req.lowStockThreshold() < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lowStockThreshold must be >= 0");
    }
    mapper.applyPatch(p, req);
    return p;
  }

  @Transactional(readOnly = true)
  public boolean existsByCategoryIdAndActiveTrue(UUID categoryId) {
    return repository.existsByCategoryIdAndActiveTrue(categoryId);
  }

  @Transactional(readOnly = true)
  public StockStatus computeStatus(Product p) {
    int stock = p.getStock() == null ? 0 : p.getStock();
    if (stock <= 0) {
      return StockStatus.OUT_OF_STOCK;
    }
    int threshold = p.getLowStockThreshold() != null
        ? p.getLowStockThreshold()
        : stockProperties.getDefaultLowThreshold();
    if (stock <= threshold) {
      return StockStatus.LOW_STOCK;
    }
    return StockStatus.IN_STOCK;
  }

  @Transactional(readOnly = true)
  public Page<Product> searchByName(String name, Pageable pageable) {
    return repository.findByNameContainingIgnoreCaseAndActiveTrue(name, pageable);
  }
}
