package com.distribuidora.exception;

/**
 * Thrown when attempting to create or update a delivery method
 * with a name that already exists.
 */
public class DuplicateDeliveryMethodException extends RuntimeException {

    private final String name;

    public DuplicateDeliveryMethodException(String name) {
        super("Delivery method already exists with name: " + name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
