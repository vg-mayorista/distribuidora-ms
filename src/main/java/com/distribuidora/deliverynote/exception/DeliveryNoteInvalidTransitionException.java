package com.distribuidora.deliverynote.exception;

import com.distribuidora.deliverynote.model.DeliveryNoteStatus;

public class DeliveryNoteInvalidTransitionException extends RuntimeException {
    public DeliveryNoteInvalidTransitionException(DeliveryNoteStatus from, DeliveryNoteStatus to) {
        super("Transición de estado inválida: " + from + " → " + to);
    }
}
