package com.distribuidora.controller;

import com.distribuidora.dto.delivery.CreateDeliveryMethodRequest;
import com.distribuidora.dto.delivery.DeliveryMethodResponse;
import com.distribuidora.dto.delivery.PatchDeliveryMethodRequest;
import com.distribuidora.dto.delivery.UpdateDeliveryMethodRequest;
import com.distribuidora.model.DeliveryMethod;
import com.distribuidora.service.DeliveryMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for {@link com.distribuidora.model.DeliveryMethod}.
 *
 * <p>Base path: {@code /api/delivery-methods}.
 */
@RestController
@RequestMapping("/api/delivery-methods")
@RequiredArgsConstructor
@Tag(name = "Métodos de Entrega", description = "Controlador para la gestión de métodos de entrega (ABM)")
public class DeliveryMethodController {

    private final DeliveryMethodService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DISTRIBUTOR')")
    @Operation(summary = "Crear método de entrega",
        description = "Registra un nuevo método de entrega en el sistema. El nombre debe ser único.")
    @ApiResponse(responseCode = "201", description = "Método de entrega creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Campos inválidos en la petición")
    @ApiResponse(responseCode = "409", description = "El nombre del método ya existe")
    public ResponseEntity<DeliveryMethodResponse> create(
            @Valid @RequestBody CreateDeliveryMethodRequest req,
            UriComponentsBuilder uriBuilder) {
        DeliveryMethod created = service.create(req);
        URI location = uriBuilder.path("/api/delivery-methods/{id}")
            .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location)
            .body(DeliveryMethodResponse.from(created));
    }

    @GetMapping
    @PreAuthorize("permitAll()")  // Públicos para checkout
    @Operation(summary = "Listar métodos de entrega",
        description = "Recupera una lista paginada de métodos de entrega. Por defecto solo devuelve los activos.")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    public Page<DeliveryMethodResponse> list(
            @Parameter(description = "Filtrar por estado (true = activos, false = inactivos)")
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @Parameter(description = "Configuración de paginación (page, size, sort)")
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.list(pageable, active).map(DeliveryMethodResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")  // Públicos para checkout
    @Operation(summary = "Obtener método de entrega por ID",
        description = "Recupera los detalles de un método de entrega activo específico.")
    @ApiResponse(responseCode = "200", description = "Método de entrega encontrado")
    @ApiResponse(responseCode = "404", description = "Método de entrega no encontrado o está inactivo")
    public DeliveryMethodResponse getById(
            @Parameter(description = "ID del método de entrega (UUID)")
            @PathVariable UUID id) {
        return DeliveryMethodResponse.from(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DISTRIBUTOR')")
    @Operation(summary = "Reemplazar método de entrega (PUT)",
        description = "Actualiza todos los atributos de un método de entrega existente.")
    @ApiResponse(responseCode = "200", description = "Método de entrega actualizado exitosamente")
    @ApiResponse(responseCode = "400", description = "Campos de entrada inválidos")
    @ApiResponse(responseCode = "404", description = "Método de entrega no encontrado o está inactivo")
    @ApiResponse(responseCode = "409", description = "El nuevo nombre ya está en uso")
    public DeliveryMethodResponse update(
            @Parameter(description = "ID del método de entrega a actualizar")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryMethodRequest req) {
        return DeliveryMethodResponse.from(service.update(id, req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DISTRIBUTOR')")
    @Operation(summary = "Actualizar parcialmente método de entrega (PATCH)",
        description = "Actualiza solo los campos proporcionados en la petición.")
    @ApiResponse(responseCode = "200", description = "Método de entrega actualizado exitosamente")
    @ApiResponse(responseCode = "404", description = "Método de entrega no encontrado o está inactivo")
    @ApiResponse(responseCode = "409", description = "El nuevo nombre ya está en uso")
    public DeliveryMethodResponse patch(
            @Parameter(description = "ID del método de entrega a actualizar")
            @PathVariable UUID id,
            @Valid @RequestBody PatchDeliveryMethodRequest req) {
        return DeliveryMethodResponse.from(service.patch(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DISTRIBUTOR')")
    @Operation(summary = "Baja lógica de método de entrega (Soft Delete)",
        description = "Desactiva el método de entrega (active = false). Operación idempotente.")
    @ApiResponse(responseCode = "204", description = "Método de entrega desactivado exitosamente (idempotente)")
    @ApiResponse(responseCode = "404", description = "Método de entrega no encontrado")
    public ResponseEntity<Void> softDelete(
            @Parameter(description = "ID del método de entrega a desactivar")
            @PathVariable UUID id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DISTRIBUTOR')")
    @Operation(summary = "Reactivar método de entrega",
        description = "Vuelve a activar un método de entrega que había sido desactivado.")
    @ApiResponse(responseCode = "200", description = "Método de entrega reactivado exitosamente")
    @ApiResponse(responseCode = "404", description = "Método de entrega no encontrado")
    public DeliveryMethodResponse activate(
            @Parameter(description = "ID del método de entrega a activar")
            @PathVariable UUID id) {
        return DeliveryMethodResponse.from(service.activate(id));
    }
}
