package com.distribuidora.controller;

import com.distribuidora.model.Product;
import com.distribuidora.service.ProductService;
import com.distribuidora.dto.product.CreateProductRequest;
import com.distribuidora.dto.product.PatchProductRequest;
import com.distribuidora.dto.product.ProductResponse;
import com.distribuidora.dto.product.UpdateProductRequest;
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

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Controlador para la gestión del catálogo de productos (ABM)")
public class ProductController {

    private final ProductService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
    @Operation(
        summary = "Crear un producto",
        description = "Registra un nuevo producto en el catálogo. El nombre debe ser único."
    )
    @ApiResponse(responseCode = "201", description = "Producto creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Campos inválidos en la petición")
    @ApiResponse(responseCode = "409", description = "El nombre del producto ya existe")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest req,
                                                  UriComponentsBuilder uriBuilder) {
        Product created = service.create(req);
        URI location = uriBuilder.path("/api/products/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(created));
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Listar productos activos paginados")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    public Page<ProductResponse> list(
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.list(pageable).map(ProductResponse::from);
    }

    @GetMapping("/search")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Buscar productos por nombre (case-insensitive)")
    public Page<ProductResponse> search(
            @Parameter(description = "Texto a buscar en el nombre del producto")
            @RequestParam String name,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(size = 5) Pageable pageable) {
        return service.searchByName(name, pageable).map(ProductResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Obtener un producto por ID")
    public ProductResponse getById(
            @Parameter(description = "ID del producto (UUID v4)")
            @PathVariable UUID id) {
        return ProductResponse.from(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
    @Operation(summary = "Reemplazar un producto (PUT)")
    public ProductResponse update(
            @Parameter(description = "ID del producto a actualizar")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest req) {
        return ProductResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
    @Operation(summary = "Baja lógica de un producto (Soft Delete)")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
    @Operation(summary = "Reactivar un producto")
    public ProductResponse activate(@PathVariable UUID id) {
        return ProductResponse.from(service.activate(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
    @Operation(summary = "Actualización parcial de un producto (PATCH)")
    public ProductResponse patch(
            @PathVariable UUID id,
            @RequestBody PatchProductRequest req) {
        return ProductResponse.from(service.patch(id, req));
    }
}
