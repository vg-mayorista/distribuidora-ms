package com.distribuidora.exception;

import java.time.LocalDate;

/**
 * El cliente intentó confirmar o actualizar un pedido con una fecha de entrega cuyo
 * cutoff semanal ya pasó. Sugerencia: refrescar el listado de fechas disponibles en el front.
 */
public class DeliveryWindowExpiredException extends RuntimeException {

    private final LocalDate deliveryDate;

    public DeliveryWindowExpiredException(LocalDate deliveryDate) {
        super("La fecha de entrega " + deliveryDate
                + " no tiene una ventana semanal abierta o su cutoff ya pasó.");
        this.deliveryDate = deliveryDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }
}
