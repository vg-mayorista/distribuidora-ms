package com.distribuidora.exception;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Alguna línea del pedido no alcanza el mínimo de unidades físicas por línea
 * definido en {@code BusinessConfig.minPacksPerLine}. El nombre del campo
 * conserva "Packs" por compatibilidad con la migración, pero la regla real es
 * "unidades físicas" (packs × unitsPerPack).
 *
 * <p>Regla: cada ítem del carrito debe tener al menos
 * {@link com.distribuidora.model.BusinessConfig#getMinPacksPerLine()} unidades
 * físicas (no packs).
 */
@Getter
public class MinPacksPerLineException extends RuntimeException {

    private final List<OffendingLine> offending;

    public MinPacksPerLineException(List<OffendingLine> offending, int minUnitsPerLine) {
        super(buildMessage(offending, minUnitsPerLine));
        this.offending = offending;
    }

    public List<OffendingLine> getOffending() {
        return offending;
    }

    private static String buildMessage(List<OffendingLine> lines, int minUnitsPerLine) {
        StringBuilder sb = new StringBuilder("Cada línea del pedido debe tener al menos ")
                .append(minUnitsPerLine)
                .append(" unidades físicas. Líneas que no cumplen:");
        for (OffendingLine l : lines) {
            sb.append(String.format(" %s (%d/%d u.);", l.productName(), l.requestedUnits(), minUnitsPerLine));
        }
        return sb.toString();
    }

    public record OffendingLine(
            UUID productId,
            String productName,
            int requestedUnits,
            int requestedPacks,
            int minRequiredUnits
    ) {}
}
