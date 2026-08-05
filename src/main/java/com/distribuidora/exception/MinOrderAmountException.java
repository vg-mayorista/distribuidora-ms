package com.distribuidora.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * El subtotal del pedido no alcanza el mínimo configurado en
 * {@code BusinessConfig.minOrderAmount}.
 */
@Getter
public class MinOrderAmountException extends RuntimeException {

    private final BigDecimal currentAmount;
    private final BigDecimal minAmount;

    public MinOrderAmountException(BigDecimal currentAmount, BigDecimal minAmount) {
        super(String.format(
                "El pedido no alcanza el monto mínimo. Subtotal actual: $%s, mínimo requerido: $%s.",
                currentAmount, minAmount));
        this.currentAmount = currentAmount;
        this.minAmount = minAmount;
    }
}
