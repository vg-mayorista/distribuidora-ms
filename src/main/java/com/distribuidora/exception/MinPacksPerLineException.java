package com.distribuidora.exception;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Alguna línea del pedido no alcanza el mínimo de packs por línea definido en
 * {@code BusinessConfig.minPacksPerLine}. El cliente tiene que sumar packs hasta
 * llegar al mínimo o cambiar el producto.
 *
 * <p>Regla: cada ítem del carrito debe tener al menos
 * {@link com.distribuidora.model.BusinessConfig#getMinPacksPerLine()} packs.
 */
@Getter
public class MinPacksPerLineException extends RuntimeException {

    private final List<OffendingLine> offending;

    public MinPacksPerLineException(List<OffendingLine> offending, int minPacksPerLine) {
        super(buildMessage(offending, minPacksPerLine));
        this.offending = offending;
    }

    public List<OffendingLine> getOffending() {
        return offending;
    }

    private static String buildMessage(List<OffendingLine> lines, int minPacksPerLine) {
        StringBuilder sb = new StringBuilder("Cada línea del pedido debe tener al menos ")
                .append(minPacksPerLine)
                .append(" packs. Líneas que no cumplen:");
        for (OffendingLine l : lines) {
            sb.append(String.format(" %s (%d/%d);", l.productName(), l.requestedPacks(), minPacksPerLine));
        }
        return sb.toString();
    }

    public record OffendingLine(UUID productId, String productName, int requestedPacks, int minRequiredPacks) {}
}
