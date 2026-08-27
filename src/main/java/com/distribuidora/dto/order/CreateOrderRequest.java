package com.distribuidora.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull
        UUID deliveryMethodId,

        /**
         * Si está presente, el pedido se trata como mayorista (a fábrica): se exige que la
         * fecha esté dentro de una ventana semanal abierta. Si es {@code null}, el pedido
         * se trata como de stock (excedente): sin fecha de entrega.
         */
        LocalDate deliveryDate,

        @Size(max = 500)
        String deliveryAddress,

        @Size(max = 50)
        String deliveryPhone,

        @Size(max = 1000)
        String notes,

        com.distribuidora.model.PaymentMethod paymentMethod,

        @Size(max = 10000000)
        String paymentReceiptUrl,

        @NotNull
        @NotEmpty
        @Valid
        List<OrderItemRequest> items
) {
    public CreateOrderRequest(UUID deliveryMethodId, LocalDate deliveryDate, String deliveryAddress, String deliveryPhone, String notes, List<OrderItemRequest> items) {
        this(deliveryMethodId, deliveryDate, deliveryAddress, deliveryPhone, notes, com.distribuidora.model.PaymentMethod.EFECTIVO, null, items);
    }

    public CreateOrderRequest(UUID deliveryMethodId, LocalDate deliveryDate, String deliveryAddress, String deliveryPhone, String notes, com.distribuidora.model.PaymentMethod paymentMethod, List<OrderItemRequest> items) {
        this(deliveryMethodId, deliveryDate, deliveryAddress, deliveryPhone, notes, paymentMethod, null, items);
    }

    public record OrderItemRequest(
            @NotNull
            UUID productId,

            @NotNull
            @DecimalMin(value = "1", inclusive = true)
            Integer quantity
    ) {}
}
