package com.distribuidora.controller;

import com.distribuidora.exception.ProductNotFoundException;
import com.distribuidora.model.Product;
import com.distribuidora.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Carrito", description = "Endpoints auxiliares para el carrito del cliente")
public class CartController {

    private final ProductRepository productRepository;

    @PostMapping("/check-stock")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Consultar stock actual de uno o más productos",
        description = "Devuelve el stock actual en unidades físicas para los IDs provistos. Pensado para que el cliente bloquee el input del carrito en vivo sin recargar el catálogo."
    )
    public ResponseEntity<Map<UUID, Integer>> checkStock(@RequestBody List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }
        Map<UUID, Integer> result = new LinkedHashMap<>();
        for (UUID id : productIds) {
            Product p = productRepository.findByIdAndActiveTrue(id)
                    .orElseThrow(() -> new ProductNotFoundException(id));
            result.put(p.getId(), p.getStock() != null ? p.getStock() : 0);
        }
        return ResponseEntity.ok(result);
    }
}
