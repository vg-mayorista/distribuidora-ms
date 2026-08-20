package com.distribuidora.deliverynote.exception;

import com.distribuidora.model.OrderStatus;

import java.util.UUID;

public class DeliveryNoteOrderInvalidStatusException extends RuntimeException {
    public DeliveryNoteOrderInvalidStatusException(UUID orderId, OrderStatus actualStatus) {
        super("Solo se puede generar remito desde órdenes ARMADO o ENVIADO. Pedido " + orderId + " está en estado " + actualStatus + ".");
    }
}
