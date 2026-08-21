package com.distribuidora.deliverynote.controller;

import com.distribuidora.dto.deliverynote.DeliveryNoteResponse;
import com.distribuidora.deliverynote.model.DeliveryNoteStatus;
import com.distribuidora.deliverynote.service.DeliveryNoteDocxService;
import com.distribuidora.deliverynote.service.DeliveryNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/distributor/delivery-notes")
@PreAuthorize("hasRole('ROLE_DISTRIBUTOR')")
@Tag(name = "Remitos (Distribuidor)", description = "Consulta y descarga de remitos de entrega")
public class DistributorDeliveryNoteController {

    private final DeliveryNoteService deliveryNoteService;
    private final DeliveryNoteDocxService deliveryNoteDocxService;

    public DistributorDeliveryNoteController(DeliveryNoteService deliveryNoteService,
                                              DeliveryNoteDocxService deliveryNoteDocxService) {
        this.deliveryNoteService = deliveryNoteService;
        this.deliveryNoteDocxService = deliveryNoteDocxService;
    }

    @GetMapping
    @Operation(summary = "Listado paginado de remitos")
    public Page<DeliveryNoteResponse> list(
            @RequestParam(required = false) DeliveryNoteStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (status != null) {
            return deliveryNoteService.listByStatus(status, pageable);
        }
        return deliveryNoteService.listAll(pageable);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Remitos de una orden específica")
    public Page<DeliveryNoteResponse> listByOrder(
            @PathVariable UUID orderId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return deliveryNoteService.listByOrder(orderId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de remito")
    public DeliveryNoteResponse get(@PathVariable UUID id) {
        return deliveryNoteService.get(id);
    }

    @GetMapping(value = "/{id}/download", produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    @Operation(summary = "Descargar remito como DOCX")
    public ResponseEntity<ByteArrayResource> download(@PathVariable UUID id) {
        byte[] docx = deliveryNoteDocxService.generate(id);
        ByteArrayResource resource = new ByteArrayResource(docx);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"remito-" + id + ".docx\"")
                .body(resource);
    }

    @PostMapping("/generate/{orderId}")
    @Operation(summary = "Generar remito desde una orden ARMADA")
    public DeliveryNoteResponse generate(@PathVariable UUID orderId) {
        return deliveryNoteService.generateFromOrder(orderId);
    }
}
