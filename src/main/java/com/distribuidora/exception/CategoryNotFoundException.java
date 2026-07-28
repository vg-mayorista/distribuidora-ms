package com.distribuidora.exception;

import java.util.UUID;

/**
 * Thrown when a {@link com.distribuidora.model.Category} is not found or is inactive
 * during a product create/update/patch operation that requires a valid FK reference.
 */
public class CategoryNotFoundException extends RuntimeException {

    private final UUID categoryId;

    public CategoryNotFoundException(UUID categoryId) {
        super("Category not found: " + categoryId);
        this.categoryId = categoryId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }
}
