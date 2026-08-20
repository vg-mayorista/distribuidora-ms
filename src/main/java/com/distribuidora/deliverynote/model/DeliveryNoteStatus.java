package com.distribuidora.deliverynote.model;

public enum DeliveryNoteStatus {
    PENDING,
    GENERATED,
    DELIVERED,
    CANCELED;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELED;
    }

    public boolean canTransitionTo(DeliveryNoteStatus target) {
        return switch (this) {
            case PENDING    -> target == GENERATED || target == CANCELED;
            case GENERATED  -> target == DELIVERED || target == CANCELED;
            case DELIVERED, CANCELED -> false;
        };
    }
}
