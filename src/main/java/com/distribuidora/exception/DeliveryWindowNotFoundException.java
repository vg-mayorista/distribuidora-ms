package com.distribuidora.exception;

import java.util.UUID;

public class DeliveryWindowNotFoundException extends RuntimeException {
    public DeliveryWindowNotFoundException(UUID id) {
        super("Ventana de entrega no encontrada: " + id);
    }
}
