package com.distribuidora.model;

public enum OrderStatus {
    PENDIENTE,
    ARMADO,
    ENVIADO,
    ENTREGADO,
    CANCELADO;

    public boolean isTerminal() {
        return this == ENTREGADO || this == CANCELADO;
    }

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDIENTE -> target == ARMADO || target == CANCELADO;
            case ARMADO    -> target == ENVIADO || target == CANCELADO;
            case ENVIADO   -> target == ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
