package com.distribuidora.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(java.util.UUID id) {
        super("Pedido no encontrado: " + id);
    }
}
