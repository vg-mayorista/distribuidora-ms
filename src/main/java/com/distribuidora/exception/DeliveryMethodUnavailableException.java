package com.distribuidora.exception;

import java.util.UUID;

/**
 * Thrown when attempting to checkout with an inactive delivery method.
 */
public class DeliveryMethodUnavailableException extends RuntimeException {

    private final UUID id;

    public DeliveryMethodUnavailableException(UUID id) {
        super("Delivery method is inactive: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
