package com.distribuidora.controller;

import com.distribuidora.dto.delivery.CreateDeliveryWindowRequest;
import com.distribuidora.dto.delivery.DeliveryWindowResponse;
import com.distribuidora.dto.delivery.UpdateDeliveryWindowRequest;
import com.distribuidora.service.DeliveryWindowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/delivery-windows")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Delivery Windows (Admin)", description = "ABM de las ventanas semanales de entrega")
public class AdminDeliveryWindowController {

    private final DeliveryWindowService service;

    @GetMapping
    @Operation(summary = "Listar todas las ventanas (incluyendo inactivas)")
    public List<DeliveryWindowResponse> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una ventana por id")
    public DeliveryWindowResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Crear una ventana semanal")
    public ResponseEntity<DeliveryWindowResponse> create(
            @Valid @RequestBody CreateDeliveryWindowRequest req) {
        DeliveryWindowResponse created = service.create(req);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una ventana (campos opcionales tipo PATCH)")
    public DeliveryWindowResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryWindowRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una ventana")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
