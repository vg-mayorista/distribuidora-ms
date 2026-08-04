package com.distribuidora.exception;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class MinOrderRequirementsNotMetException extends RuntimeException {

    private final BigDecimal currentAmount;
    private final BigDecimal minAmount;
    private final int currentUnits;
    private final int minUnits;

    public MinOrderRequirementsNotMetException(BigDecimal currentAmount, BigDecimal minAmount, int currentUnits, int minUnits) {
        super(buildMessage(currentAmount, minAmount, currentUnits, minUnits));
        this.currentAmount = currentAmount;
        this.minAmount = minAmount;
        this.currentUnits = currentUnits;
        this.minUnits = minUnits;
    }

    private static String buildMessage(BigDecimal currentAmount, BigDecimal minAmount, int currentUnits, int minUnits) {
        StringBuilder sb = new StringBuilder("El pedido no cumple con las condiciones mínimas de compra:");
        if (currentAmount.compareTo(minAmount) < 0) {
            sb.append(String.format(" Monto actual: $%s, Monto mínimo requerido: $%s.", currentAmount, minAmount));
        }
        if (currentUnits < minUnits) {
            sb.append(String.format(" Unidades actuales: %d, Unidades mínimas requeridas: %d.", currentUnits, minUnits));
        }
        return sb.toString();
    }
}
