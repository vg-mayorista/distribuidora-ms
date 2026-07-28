package com.distribuidora.exception;

import java.util.UUID;

/**
 * Thrown when attempting to soft-delete a {@link com.distribuidora.model.Category}
 * that still has active {@link com.distribuidora.model.Product} references.
 *
 * <p>Business rule BR-002: cannot deactivate a category with active products.
 */
public class CategoryHasProductsException extends RuntimeException {

    private final UUID categoryId;

    public CategoryHasProductsException(UUID categoryId) {
        super("Cannot deactivate category " + categoryId + ": it has active products");
        this.categoryId = categoryId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }
}
