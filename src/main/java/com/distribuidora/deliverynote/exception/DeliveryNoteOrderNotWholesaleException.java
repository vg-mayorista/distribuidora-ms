package com.distribuidora.deliverynote.exception;

import com.distribuidora.model.OrderType;

import java.util.UUID;

public class DeliveryNoteOrderNotWholesaleException extends RuntimeException {
    public DeliveryNoteOrderNotWholesaleException(UUID orderId, OrderType actualType) {
        super("Solo se puede generar remito para pedidos mayoristas. Pedido " + orderId + " es de tipo " + actualType + ".");
    }
}
