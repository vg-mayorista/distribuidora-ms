package com.distribuidora.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull
        UUID deliveryMethodId,

        @FutureOrPresent
        LocalDate deliveryDate,

        @Size(max = 500)
        String deliveryAddress,

        @Size(max = 50)
        String deliveryPhone,

        @Size(max = 1000)
        String notes,

        @NotNull
        @NotEmpty
        @Valid
        List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotNull
            UUID productId,

            @NotNull
            @DecimalMin(value = "1", inclusive = true)
            Integer quantity
    ) {}
}
