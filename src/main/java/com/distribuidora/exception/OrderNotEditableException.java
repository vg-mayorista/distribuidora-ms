package com.distribuidora.exception;

public class OrderNotEditableException extends RuntimeException {
    public OrderNotEditableException(String message) {
        super(message);
    }
}
