package com.distribuidora.controller;
import com.distribuidora.model.Category;
import com.distribuidora.service.CategoryService;

import com.distribuidora.dto.category.CategoryResponse;
import com.distribuidora.dto.category.CreateCategoryRequest;
import com.distribuidora.dto.category.UpdateCategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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
 * REST controller for the {@link Category} entity.
 *
 * <p>Base path: {@code /api/categories}.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categorías", description = "Controlador para la gestión de categorías del catálogo (ABM)")
public class CategoryController {

    private final CategoryService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
    @Operation(
        summary = "Crear una categoría",
        description = "Registra una nueva categoría en el catálogo. El nombre debe ser único (case-sensitive)."
    )
    @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente")
    @ApiResponse(responseCode = "400", description = "Campos inválidos en la petición")
    @ApiResponse(responseCode = "409", description = "El nombre de la categoría ya existe")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest req,
                                                   UriComponentsBuilder uriBuilder) {
        Category created = service.create(req);
        URI location = uriBuilder.path("/api/categories/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(CategoryResponse.from(created));
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Listar categorías",
        description = "Recupera una lista paginada de categorías. Por defecto solo devuelve las activas."
    )
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    public Page<CategoryResponse> list(
            @Parameter(description = "Filtrar por estado (true = activas, false = inactivas)")
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @Parameter(description = "Configuración de paginación (page, size, sort)")
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.list(pageable, active).map(CategoryResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Obtener una categoría por ID",
        description = "Recupera los detalles de una categoría activa específica usando su identificador UUID."
    )
    @ApiResponse(responseCode = "200", description = "Categoría encontrada")
    @ApiResponse(responseCode = "404", description = "Categoría no encontrada o está inactiva")
    public CategoryResponse getById(
            @Parameter(description = "ID de la categoría (UUID)", example = "a3f2b8c1-7d4e-4f1a-b5c3-9e8d2f6a1b4c")
            @PathVariable UUID id) {
        return CategoryResponse.from(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
    @Operation(
        summary = "Reemplazar una categoría (PUT)",
        description = "Actualiza todos los atributos de una categoría existente. Requiere todos los campos."
    )
    @ApiResponse(responseCode = "200", description = "Categoría actualizada exitosamente")
    @ApiResponse(responseCode = "400", description = "Campos de entrada inválidos")
    @ApiResponse(responseCode = "404", description = "Categoría no encontrada o está inactiva")
    @ApiResponse(responseCode = "409", description = "El nuevo nombre de la categoría ya está en uso")
    public CategoryResponse update(
            @Parameter(description = "ID de la categoría a actualizar (UUID)", example = "a3f2b8c1-7d4e-4f1a-b5c3-9e8d2f6a1b4c")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest req) {
        return CategoryResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
    @Operation(
        summary = "Baja lógica de una categoría (Soft Delete)",
        description = "Desactiva la categoría (active = false). No permite la baja si existen productos activos asociados."
    )
    @ApiResponse(responseCode = "204", description = "Categoría desactivada exitosamente (idempotente)")
    @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    @ApiResponse(responseCode = "409", description = "La categoría tiene productos activos y no puede ser desactivada")
    public ResponseEntity<Void> softDelete(
            @Parameter(description = "ID de la categoría a desactivar (UUID)", example = "a3f2b8c1-7d4e-4f1a-b5c3-9e8d2f6a1b4c")
            @PathVariable UUID id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
    @Operation(
        summary = "Reactivar una categoría",
        description = "Vuelve a activar una categoría que había sido dada de baja lógicamente (active = true)."
    )
    @ApiResponse(responseCode = "200", description = "Categoría reactivada exitosamente")
    @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    public CategoryResponse activate(
            @Parameter(description = "ID de la categoría a activar (UUID)", example = "a3f2b8c1-7d4e-4f1a-b5c3-9e8d2f6a1b4c")
            @PathVariable UUID id) {
        return CategoryResponse.from(service.activate(id));
    }
}
