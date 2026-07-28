package com.distribuidora.exception;

/**
 * Thrown when a product name already exists in the catalog.
 */
public class DuplicateProductException extends RuntimeException {

    private final String name;

    public DuplicateProductException(String name) {
        super("Product already exists with name: '" + name + "'");
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
