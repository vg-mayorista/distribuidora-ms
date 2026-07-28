package com.distribuidora.controller;

import com.distribuidora.dto.user.CustomerSummaryResponse;
import com.distribuidora.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Administración de Clientes", description = "Endpoints para la gestión de clientes (sólo Admin)")
public class AdminCustomerController {

    private final UserService userService;

    @GetMapping
    @Operation(
        summary = "Listar clientes paginados",
        description = "Devuelve una lista paginada de todos los usuarios con rol de cliente (ROLE_CUSTOMER)."
    )
    @ApiResponse(responseCode = "200", description = "Lista de clientes cargada exitosamente")
    public ResponseEntity<Page<CustomerSummaryResponse>> getCustomers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getCustomers(search, pageable));
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(
        summary = "Habilitar/Deshabilitar cliente",
        description = "Invierte el estado activo/inactivo del cliente indicado por su UUID."
    )
    @ApiResponse(responseCode = "200", description = "Estado del cliente modificado exitosamente")
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    public ResponseEntity<CustomerSummaryResponse> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.toggleCustomerActive(id));
    }
}
