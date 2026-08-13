package com.distribuidora.exception;

import java.util.UUID;

public class NotificationLogNotFoundException extends RuntimeException {
    public NotificationLogNotFoundException(UUID id) {
        super("Notificación no encontrada con id: " + id);
    }
}
