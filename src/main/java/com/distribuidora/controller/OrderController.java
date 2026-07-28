package com.distribuidora.controller;

import com.distribuidora.config.security.CustomUserDetails;
import com.distribuidora.dto.order.CreateOrderRequest;
import com.distribuidora.dto.order.OrderResponse;
import com.distribuidora.dto.order.UpdateOrderRequest;
import com.distribuidora.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_CUSTOMER')")
@Tag(name = "Pedidos (Cliente)", description = "ABM de los pedidos del cliente autenticado")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Crear pedido")
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateOrderRequest req,
            UriComponentsBuilder uriBuilder) {
        OrderResponse created = orderService.create(user.getUser().getId(), req);
        URI location = uriBuilder.path("/api/orders/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar mis pedidos")
    public Page<OrderResponse> listMine(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.listMine(user.getUser().getId(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de mi pedido")
    public OrderResponse getMine(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id) {
        return orderService.getMine(user.getUser().getId(), id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Modificar mi pedido (solo si está PENDIENTE y antes de la fecha de edición)")
    public OrderResponse updateMine(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderRequest req) {
        return orderService.updateMine(user.getUser().getId(), id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar mi pedido (solo si está PENDIENTE y antes de la fecha de edición)")
    public OrderResponse cancelMine(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id) {
        return orderService.cancelMine(user.getUser().getId(), id);
    }
}
