package com.distribuidora.deliverynote.exception;

public class DeliveryNoteNotFoundException extends RuntimeException {
    public DeliveryNoteNotFoundException(java.util.UUID id) {
        super("Remito no encontrado: " + id);
    }
}
