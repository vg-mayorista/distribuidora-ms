package com.distribuidora.controller;

import com.distribuidora.dto.order.OrderResponse;
import com.distribuidora.dto.order.UpdateOrderStatusRequest;
import com.distribuidora.model.OrderStatus;
import com.distribuidora.repository.OrderRepository;
import com.distribuidora.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/distributor/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_DISTRIBUTOR')")
@Tag(name = "Pedidos (Distribuidor)", description = "Gestión de pedidos recibidos (cambiar estado, marcar armado/enviado/entregado)")
public class DistributorOrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @GetMapping
    @Operation(summary = "Listar todos los pedidos (con filtros opcionales)")
    public Page<OrderResponse> list(
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (deliveryDate != null && statuses != null && !statuses.isEmpty()) {
            return orderRepository.findByDeliveryDateAndStatus(deliveryDate, statuses.get(0), pageable)
                    .map(orderService::toResponsePublic);
        }
        if (deliveryDate != null) {
            return orderRepository.findByDeliveryDate(deliveryDate, pageable).map(orderService::toResponsePublic);
        }
        if (statuses != null && !statuses.isEmpty()) {
            return orderRepository.findByStatusIn(statuses, pageable).map(orderService::toResponsePublic);
        }
        return orderService.listAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un pedido")
    public OrderResponse get(@PathVariable UUID id) {
        return orderService.get(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar estado del pedido (PENDIENTE → ARMADO → ENVIADO → ENTREGADO / CANCELADO)")
    public OrderResponse transition(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest req) {
        return orderService.transitionStatus(id, req);
    }

    @PatchMapping("/{id}/delivery-date")
    @Operation(summary = "Asignar/Modificar fecha de reparto del pedido")
    public OrderResponse updateDeliveryDate(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> body) {
        java.time.LocalDate date = body.containsKey("deliveryDate") && body.get("deliveryDate") != null && !body.get("deliveryDate").isBlank()
                ? java.time.LocalDate.parse(body.get("deliveryDate"))
                : null;
        return orderService.updateDeliveryDate(id, date);
    }
}
