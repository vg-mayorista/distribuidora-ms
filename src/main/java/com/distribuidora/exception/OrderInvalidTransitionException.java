package com.distribuidora.exception;

import com.distribuidora.model.OrderStatus;

public class OrderInvalidTransitionException extends RuntimeException {
    public OrderInvalidTransitionException(OrderStatus from, OrderStatus to) {
        super("Transición inválida: " + from + " → " + to);
    }
}
