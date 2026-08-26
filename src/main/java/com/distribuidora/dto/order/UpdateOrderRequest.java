package com.distribuidora.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateOrderRequest(

        UUID deliveryMethodId,

        LocalDate deliveryDate,

        @Size(max = 500)
        String deliveryAddress,

        @Size(max = 50)
        String deliveryPhone,

        @Size(max = 1000)
        String notes,

        com.distribuidora.model.PaymentMethod paymentMethod,

        @NotNull
        @NotEmpty
        @Valid
        List<OrderItemRequest> items
) {
    public UpdateOrderRequest(UUID deliveryMethodId, LocalDate deliveryDate, String deliveryAddress, String deliveryPhone, String notes, List<OrderItemRequest> items) {
        this(deliveryMethodId, deliveryDate, deliveryAddress, deliveryPhone, notes, null, items);
    }

    public record OrderItemRequest(
            @NotNull
            UUID productId,
            @NotNull
            Integer quantity
    ) {}
}
