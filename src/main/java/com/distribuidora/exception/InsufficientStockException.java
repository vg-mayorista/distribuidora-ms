package com.distribuidora.exception;

import java.util.List;
import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    private final List<InsufficientStockItem> items;

    public InsufficientStockException(List<InsufficientStockItem> items) {
        super("Stock insuficiente para uno o más productos.");
        this.items = items;
    }

    public List<InsufficientStockItem> getItems() {
        return items;
    }

    public record InsufficientStockItem(UUID productId, String productName, int requested, int available) {}
}
