package com.distribuidora.exception;

import java.util.UUID;

/**
 * Thrown when a {@link com.distribuidora.model.Product} cannot be found by its id.
 */
public class ProductNotFoundException extends RuntimeException {

    private final UUID id;

    public ProductNotFoundException(UUID id) {
        super("Product not found: id=" + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
