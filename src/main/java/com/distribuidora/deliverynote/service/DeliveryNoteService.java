package com.distribuidora.deliverynote.service;

import com.distribuidora.deliverynote.dto.CreateDeliveryNoteRequest;
import com.distribuidora.deliverynote.dto.DeliveryNoteItemResponse;
import com.distribuidora.deliverynote.dto.DeliveryNoteResponse;
import com.distribuidora.deliverynote.exception.DeliveryNoteInvalidTransitionException;
import com.distribuidora.deliverynote.exception.DeliveryNoteNotFoundException;
import com.distribuidora.deliverynote.exception.DeliveryNoteOrderInvalidStatusException;
import com.distribuidora.deliverynote.exception.DeliveryNoteOrderNotWholesaleException;
import com.distribuidora.deliverynote.model.DeliveryNote;
import com.distribuidora.deliverynote.model.DeliveryNoteItem;
import com.distribuidora.deliverynote.model.DeliveryNoteStatus;
import com.distribuidora.deliverynote.repository.DeliveryNoteRepository;
import com.distribuidora.exception.OrderNotFoundException;
import com.distribuidora.model.Order;
import com.distribuidora.model.OrderItem;
import com.distribuidora.model.OrderType;
import com.distribuidora.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryNoteService {

    private final DeliveryNoteRepository deliveryNoteRepository;
    private final OrderRepository orderRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private final Clock clock = Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));

    // ── Queries ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<DeliveryNoteResponse> listAll(Pageable pageable) {
        return deliveryNoteRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryNoteResponse> listByStatus(DeliveryNoteStatus status, Pageable pageable) {
        return deliveryNoteRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryNoteResponse> listByOrder(UUID orderId, Pageable pageable) {
        return deliveryNoteRepository.findByOrderId(orderId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DeliveryNoteResponse get(UUID id) {
        DeliveryNote dn = loadOrThrow(id);
        return toResponse(dn);
    }

    // ── Create ───────────────────────────────────────────────────────────

    public DeliveryNoteResponse generateFromOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != com.distribuidora.model.OrderStatus.ARMADO) {
            throw new DeliveryNoteOrderInvalidStatusException(order.getId(), order.getStatus());
        }

        if (order.getType() != OrderType.WHOLESALE) {
            throw new DeliveryNoteOrderNotWholesaleException(order.getId(), order.getType());
        }

        int year = LocalDate.now(clock).getYear();
        Page<DeliveryNote> existingNotes = deliveryNoteRepository.findByOrderId(order.getId(), org.springframework.data.domain.Pageable.unpaged());
        if (existingNotes != null && existingNotes.hasContent()) {
            throw new IllegalStateException("Ya existe un remito para el pedido " + order.getId());
        }

        DeliveryNote deliveryNote = DeliveryNote.builder()
                .orderId(order.getId())
                .deliveryNoteNumber(generateNextNumber(year))
                .status(DeliveryNoteStatus.PENDING)
                .issueDate(LocalDate.now(clock))
                .deliveryDate(order.getDeliveryDate())
                .notes("Generado desde pedido " + orderId)
                .build();

        for (OrderItem item : order.getItems()) {
            DeliveryNoteItem dni = DeliveryNoteItem.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .unitPrice(item.getUnitPrice())
                    .quantityDelivered(item.getQuantity())
                    .build();
            deliveryNote.addItem(dni);
        }

        DeliveryNote saved = deliveryNoteRepository.save(deliveryNote);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.distribuidora.deliverynote.event.DeliveryNoteCreatedEvent(
                    saved.getId(),
                    saved.getDeliveryNoteNumber(),
                    order.getUserId()));
        }

        return toResponse(saved);
    }

    // ── Status transition ────────────────────────────────────────────────

    public DeliveryNoteResponse transitionStatus(UUID id, DeliveryNoteStatus targetStatus, String notes) {
        DeliveryNote dn = loadOrThrow(id);
        DeliveryNoteStatus current = dn.getStatus();

        if (!current.canTransitionTo(targetStatus)) {
            throw new DeliveryNoteInvalidTransitionException(current, targetStatus);
        }

        if (targetStatus == DeliveryNoteStatus.DELIVERED) {
            dn.setDeliveryDate(LocalDate.now(clock));
            dn.setClosedAt(Instant.now(clock));
        }

        if (notes != null && !notes.isBlank()) {
            String existing = dn.getNotes();
            dn.setNotes(existing == null || existing.isBlank() ? notes : existing + "\n— " + notes);
        }

        dn.setStatus(targetStatus);
        return toResponse(dn);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private synchronized String generateNextNumber(int year) {
        String max = deliveryNoteRepository.findMaxDeliveryNoteNumberForYear(year)
                .orElse(null);
        int next = 1;
        if (max != null && max.matches("R-\\d{4}-\\d+")) {
            try {
                String[] parts = max.split("-");
                next = Integer.parseInt(parts[2]) + 1;
            } catch (NumberFormatException ignored) {
                next = 1;
            }
        }
        return String.format("R-%d-%04d", year, next);
    }

    private DeliveryNote loadOrThrow(UUID id) {
        return deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new DeliveryNoteNotFoundException(id));
    }

    private DeliveryNoteResponse toResponse(DeliveryNote dn) {
        List<DeliveryNoteItemResponse> itemResponses = dn.getItems().stream()
                .map(i -> new DeliveryNoteItemResponse(
                        i.getId(),
                        i.getProductId(),
                        i.getProductName(),
                        i.getUnitPrice(),
                        i.getQuantityDelivered()))
                .toList();

        return new DeliveryNoteResponse(
                dn.getId(),
                dn.getOrderId(),
                dn.getDeliveryNoteNumber(),
                dn.getStatus(),
                dn.getIssueDate(),
                dn.getDeliveryDate(),
                dn.getNotes(),
                itemResponses,
                dn.getCreatedAt(),
                dn.getUpdatedAt(),
                dn.getClosedAt()
        );
    }
}
