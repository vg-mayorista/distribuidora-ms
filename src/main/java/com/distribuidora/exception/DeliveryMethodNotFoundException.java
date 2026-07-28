package com.distribuidora.exception;

import java.util.UUID;

/**
 * Thrown when a {@link com.distribuidora.model.DeliveryMethod} is not found
 * or is inactive.
 */
public class DeliveryMethodNotFoundException extends RuntimeException {

    private final UUID id;

    public DeliveryMethodNotFoundException(UUID id) {
        super("Delivery method not found: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
