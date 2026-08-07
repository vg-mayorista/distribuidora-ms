package com.distribuidora.exception;

import java.util.UUID;

/**
 * Thrown when attempting to checkout with an unavailable delivery method.
 * Typically because the method is inactive or its scope doesn't match the order type.
 */
public class DeliveryMethodUnavailableException extends RuntimeException {

    private final UUID id;

    public DeliveryMethodUnavailableException(UUID id) {
        super("Delivery method is inactive: " + id);
        this.id = id;
    }

    public DeliveryMethodUnavailableException(String message) {
        super(message);
        this.id = null;
    }

    public UUID getId() {
        return id;
    }
}
