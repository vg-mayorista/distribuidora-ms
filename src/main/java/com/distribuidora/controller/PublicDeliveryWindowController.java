package com.distribuidora.controller;

import com.distribuidora.dto.delivery.DeliveryWindowResponse;
import com.distribuidora.service.DeliveryWindowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/config/delivery-windows")
@RequiredArgsConstructor
@Tag(name = "Delivery Windows (Público)", description = "Ventanas semanales activas para que el cliente sepa qué fechas de entrega están disponibles")
public class PublicDeliveryWindowController {

    private final DeliveryWindowService service;

    @GetMapping
    @Operation(summary = "Listar las ventanas semanales activas")
    public List<DeliveryWindowResponse> listActive() {
        return service.listActive();
    }
}
