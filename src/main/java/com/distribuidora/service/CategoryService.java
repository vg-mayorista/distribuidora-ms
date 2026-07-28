package com.distribuidora.service;

import com.distribuidora.mapper.CategoryMapper;
import com.distribuidora.model.Category;
import com.distribuidora.repository.CategoryRepository;

import com.distribuidora.dto.category.CreateCategoryRequest;
import com.distribuidora.dto.category.UpdateCategoryRequest;
import com.distribuidora.exception.CategoryHasProductsException;
import com.distribuidora.exception.CategoryNotFoundException;
import com.distribuidora.exception.DuplicateCategoryException;
import com.distribuidora.repository.ProductRepository;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business layer for the {@link Category} entity.
 *
 * <p>
 * Public reads only see {@code active = true} categories. Administrative
 * operations (deactivate / reactivate) ignore that flag.
 *
 * <p>
 * Business rule BR-002: a category with active products cannot be
 * soft-deleted.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;
    private final ProductRepository productRepository;

    public Category create(CreateCategoryRequest req) {
        if (repository.existsByName(req.name())) {
            throw new DuplicateCategoryException(req.name());
        }
        Category c = mapper.toEntity(req);
        return repository.save(c);
    }

    @Transactional(readOnly = true)
    public Category getById(UUID id) {
        return repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Category> list(Pageable pageable, Boolean active) {
        if (active == null || active) {
            return repository.findByActiveTrue(pageable);
        }
        return repository.findByActiveFalse(pageable);
    }

    public Category update(UUID id, UpdateCategoryRequest req) {
        Category c = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        if (repository.existsByNameAndIdNot(req.name(), id)) {
            throw new DuplicateCategoryException(req.name());
        }
        mapper.applyUpdate(c, req);
        return c;
    }

    public void softDelete(UUID id) {
        if (productRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new CategoryHasProductsException(id);
        }
        Category c = repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        if (Boolean.FALSE.equals(c.getActive())) {
            return;
        }
        c.setActive(Boolean.FALSE);
    }

    public Category activate(UUID id) {
        Category c = repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        c.setActive(Boolean.TRUE);
        return c;
    }
}
