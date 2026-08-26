package com.distribuidora.deliverynote.service;

import com.distribuidora.deliverynote.dto.DeliveryNoteItemResponse;
import com.distribuidora.deliverynote.dto.DeliveryNoteResponse;
import com.distribuidora.exception.OrderNotFoundException;
import com.distribuidora.model.Order;
import com.distribuidora.model.OrderItem;
import com.distribuidora.model.OrderStatus;
import com.distribuidora.model.OrderType;
import com.distribuidora.deliverynote.exception.DeliveryNoteInvalidTransitionException;
import com.distribuidora.deliverynote.exception.DeliveryNoteNotFoundException;
import com.distribuidora.deliverynote.exception.DeliveryNoteOrderInvalidStatusException;
import com.distribuidora.deliverynote.exception.DeliveryNoteOrderNotWholesaleException;
import com.distribuidora.deliverynote.model.DeliveryNote;
import com.distribuidora.deliverynote.model.DeliveryNoteItem;
import com.distribuidora.deliverynote.model.DeliveryNoteStatus;
import com.distribuidora.deliverynote.repository.DeliveryNoteRepository;
import com.distribuidora.repository.OrderRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryNoteServiceTest {

    private static final ZoneId ARG = ZoneId.of("America/Argentina/Buenos_Aires");

    @Mock
    DeliveryNoteRepository deliveryNoteRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    DeliveryNoteService service;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private Order armadoWholesaleOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .status(OrderStatus.ARMADO)
                .type(OrderType.WHOLESALE)
                .deliveryDate(LocalDate.of(2026, 8, 25))
                .items(List.of(
                        OrderItem.builder()
                                .productId(PRODUCT_ID)
                                .productName("Producto A")
                                .unitPrice(new BigDecimal("100.00"))
                                .quantity(2)
                                .subtotal(new BigDecimal("200.00"))
                                .build()
                ))
                .build();
    }

    private DeliveryNote deliveryNote(Order order) {
        List<com.distribuidora.deliverynote.model.DeliveryNoteItem> items = order.getItems().stream()
                .map(i -> com.distribuidora.deliverynote.model.DeliveryNoteItem.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice())
                        .quantityDelivered(i.getQuantity())
                        .build())
                .toList();

        return com.distribuidora.deliverynote.model.DeliveryNote.builder()
                .id(UUID.randomUUID())
                .orderId(order.getId())
                .deliveryNoteNumber("R-2026-0001")
                .status(DeliveryNoteStatus.PENDING)
                .issueDate(LocalDate.now(ARG))
                .deliveryDate(order.getDeliveryDate())
                .notes("Generado desde pedido " + order.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .items(items)
                .build();
    }

    private DeliveryNote pendingNote() {
        DeliveryNote note = deliveryNote(armadoWholesaleOrder());
        note.setStatus(DeliveryNoteStatus.PENDING);
        return note;
    }

    @Nested
    class GenerateFromOrder {

        @Test
        void createsDeliveryNoteFromArmadoWholesaleOrder() {
            Order order = armadoWholesaleOrder();
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(deliveryNoteRepository.findByOrderId(any(), any())).thenReturn(Page.empty());
            when(deliveryNoteRepository.findMaxDeliveryNoteNumberForYear(2026)).thenReturn(Optional.empty());
            when(deliveryNoteRepository.save(any(DeliveryNote.class))).thenAnswer(inv -> inv.getArgument(0));

            DeliveryNoteResponse result = service.generateFromOrder(ORDER_ID);

            assertThat(result.orderId()).isEqualTo(ORDER_ID);
            assertThat(result.status()).isEqualTo(DeliveryNoteStatus.PENDING);
            assertThat(result.deliveryNoteNumber()).isEqualTo("R-2026-0001");
            assertThat(result.deliveryDate()).isEqualTo(LocalDate.of(2026, 8, 25));
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).productName()).isEqualTo("Producto A");
            assertThat(result.items().get(0).quantityDelivered()).isEqualTo(2);
        }

        @Test
        void throwsOrderNotFoundWhenOrderMissing() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generateFromOrder(ORDER_ID))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(deliveryNoteRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void throwsInvalidStatusWhenOrderIsPendiente() {
            Order order = armadoWholesaleOrder();
            order.setStatus(OrderStatus.PENDIENTE);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.generateFromOrder(ORDER_ID))
                    .isInstanceOf(DeliveryNoteOrderInvalidStatusException.class)
                    .hasMessageContaining(ORDER_ID.toString())
                    .hasMessageContaining("PENDIENTE");

            verify(deliveryNoteRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void throwsInvalidStatusWhenOrderIsEntregado() {
            Order order = armadoWholesaleOrder();
            order.setStatus(OrderStatus.ENTREGADO);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.generateFromOrder(ORDER_ID))
                    .isInstanceOf(DeliveryNoteOrderInvalidStatusException.class)
                    .hasMessageContaining("ENTREGADO");

            verify(deliveryNoteRepository, never()).save(any());
        }

        @Test
        void throwsNotWholesaleWhenOrderIsStock() {
            Order order = armadoWholesaleOrder();
            order.setType(OrderType.STOCK);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.generateFromOrder(ORDER_ID))
                    .isInstanceOf(DeliveryNoteOrderNotWholesaleException.class)
                    .hasMessageContaining("STOCK");

            verify(deliveryNoteRepository, never()).save(any());
        }

        @Test
        void generatesFirstNumberWhenNoExistingNoteForYear() {
            Order order = armadoWholesaleOrder();
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(deliveryNoteRepository.findByOrderId(any(), any())).thenReturn(Page.empty());
            when(deliveryNoteRepository.findMaxDeliveryNoteNumberForYear(2026)).thenReturn(Optional.empty());
            when(deliveryNoteRepository.save(any(DeliveryNote.class))).thenAnswer(inv -> inv.getArgument(0));

            DeliveryNoteResponse result = service.generateFromOrder(ORDER_ID);

            assertThat(result.deliveryNoteNumber()).isEqualTo("R-2026-0001");
        }

        @Test
        void incrementsNumberFromExistingMax() {
            Order order = armadoWholesaleOrder();
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(deliveryNoteRepository.findByOrderId(any(), any())).thenReturn(Page.empty());
            when(deliveryNoteRepository.findMaxDeliveryNoteNumberForYear(2026)).thenReturn(Optional.of("R-2026-0005"));
            when(deliveryNoteRepository.save(any(DeliveryNote.class))).thenAnswer(inv -> inv.getArgument(0));

            DeliveryNoteResponse result = service.generateFromOrder(ORDER_ID);

            assertThat(result.deliveryNoteNumber()).isEqualTo("R-2026-0006");
        }

        @Test
        void publishesDeliveryNoteCreatedEvent() {
            Order order = armadoWholesaleOrder();
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(deliveryNoteRepository.findByOrderId(any(), any())).thenReturn(Page.empty());
            when(deliveryNoteRepository.findMaxDeliveryNoteNumberForYear(2026)).thenReturn(Optional.empty());
            when(deliveryNoteRepository.save(any(DeliveryNote.class))).thenAnswer(inv -> {
                DeliveryNote dn = inv.getArgument(0);
                dn.setId(UUID.randomUUID());
                return dn;
            });

            service.generateFromOrder(ORDER_ID);

            ArgumentCaptor<com.distribuidora.deliverynote.event.DeliveryNoteCreatedEvent> captor =
                    ArgumentCaptor.forClass(com.distribuidora.deliverynote.event.DeliveryNoteCreatedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            com.distribuidora.deliverynote.event.DeliveryNoteCreatedEvent event = captor.getValue();
            assertThat(event.customerUserId()).isEqualTo(USER_ID);
            assertThat(event.deliveryNoteNumber()).isEqualTo("R-2026-0001");
            assertThat(event.deliveryNoteId()).isNotNull();
        }
    }

    @Nested
    class TransitionStatus {

        @Test
        void transitionsFromPendingToGenerated() {
            DeliveryNote note = pendingNote();
            when(deliveryNoteRepository.findById(note.getId())).thenReturn(Optional.of(note));

            DeliveryNoteResponse result = service.transitionStatus(note.getId(), DeliveryNoteStatus.GENERATED, null);

            assertThat(result.status()).isEqualTo(DeliveryNoteStatus.GENERATED);
        }

        @Test
        void transitionsFromGeneratedToDelivered() {
            DeliveryNote note = pendingNote();
            note.setStatus(DeliveryNoteStatus.GENERATED);
            when(deliveryNoteRepository.findById(note.getId())).thenReturn(Optional.of(note));

            DeliveryNoteResponse result = service.transitionStatus(note.getId(), DeliveryNoteStatus.DELIVERED, null);

            assertThat(result.status()).isEqualTo(DeliveryNoteStatus.DELIVERED);
            assertThat(result.deliveryDate()).isEqualTo(LocalDate.now(ARG));
            assertThat(result.closedAt()).isNotNull();
        }

        @Test
        void transitionsFromGeneratedToCanceled() {
            DeliveryNote note = pendingNote();
            note.setStatus(DeliveryNoteStatus.GENERATED);
            when(deliveryNoteRepository.findById(note.getId())).thenReturn(Optional.of(note));

            DeliveryNoteResponse result = service.transitionStatus(note.getId(), DeliveryNoteStatus.CANCELED, null);

            assertThat(result.status()).isEqualTo(DeliveryNoteStatus.CANCELED);
        }

        @Test
        void throwsInvalidTransitionForPendingToDelivered() {
            DeliveryNote note = pendingNote();
            when(deliveryNoteRepository.findById(note.getId())).thenReturn(Optional.of(note));

            assertThatThrownBy(() -> service.transitionStatus(note.getId(), DeliveryNoteStatus.DELIVERED, null))
                    .isInstanceOf(DeliveryNoteInvalidTransitionException.class)
                    .hasMessageContaining("PENDING")
                    .hasMessageContaining("DELIVERED");
        }

        @Test
        void appendsNotesWhenProvided() {
            DeliveryNote note = pendingNote();
            note.setNotes("Nota original");
            when(deliveryNoteRepository.findById(note.getId())).thenReturn(Optional.of(note));

            DeliveryNoteResponse result = service.transitionStatus(note.getId(), DeliveryNoteStatus.GENERATED, "Nota adicional");

            assertThat(result.notes()).isEqualTo("Nota original\n— Nota adicional");
        }

        @Test
        void replacesNotesWhenOriginalIsBlank() {
            DeliveryNote note = pendingNote();
            note.setNotes(null);
            when(deliveryNoteRepository.findById(note.getId())).thenReturn(Optional.of(note));

            DeliveryNoteResponse result = service.transitionStatus(note.getId(), DeliveryNoteStatus.GENERATED, "Solo esta nota");

            assertThat(result.notes()).isEqualTo("Solo esta nota");
        }

        @Test
        void throwsNotFoundWhenDeliveryNoteMissing() {
            UUID missingId = UUID.randomUUID();
            when(deliveryNoteRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.transitionStatus(missingId, DeliveryNoteStatus.GENERATED, null))
                    .isInstanceOf(DeliveryNoteNotFoundException.class);
        }
    }

    @Nested
    class Get {

        @Test
        void returnsDeliveryNoteWhenFound() {
            DeliveryNote note = pendingNote();
            when(deliveryNoteRepository.findById(note.getId())).thenReturn(Optional.of(note));

            DeliveryNoteResponse result = service.get(note.getId());

            assertThat(result.id()).isEqualTo(note.getId());
            assertThat(result.deliveryNoteNumber()).isEqualTo(note.getDeliveryNoteNumber());
        }

        @Test
        void throwsNotFoundWhenMissing() {
            UUID missingId = UUID.randomUUID();
            when(deliveryNoteRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(missingId))
                    .isInstanceOf(DeliveryNoteNotFoundException.class);
        }
    }

    @Nested
    class Queries {

        @Test
        void listAllReturnsPage() {
            Pageable pageable = PageRequest.of(0, 20);
            DeliveryNote note = pendingNote();
            Page<DeliveryNote> page = new PageImpl<>(List.of(note), pageable, 1);
            when(deliveryNoteRepository.findAll(pageable)).thenReturn(page);

            Page<DeliveryNoteResponse> result = service.listAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).deliveryNoteNumber()).isEqualTo(note.getDeliveryNoteNumber());
        }

        @Test
        void listByStatusReturnsFilteredPage() {
            Pageable pageable = PageRequest.of(0, 20);
            DeliveryNote note = pendingNote();
            Page<DeliveryNote> page = new PageImpl<>(List.of(note), pageable, 1);
            when(deliveryNoteRepository.findByStatus(DeliveryNoteStatus.PENDING, pageable)).thenReturn(page);

            Page<DeliveryNoteResponse> result = service.listByStatus(DeliveryNoteStatus.PENDING, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).status()).isEqualTo(DeliveryNoteStatus.PENDING);
        }

        @Test
        void listByOrderReturnsFilteredPage() {
            Pageable pageable = PageRequest.of(0, 20);
            DeliveryNote note = pendingNote();
            Page<DeliveryNote> page = new PageImpl<>(List.of(note), pageable, 1);
            when(deliveryNoteRepository.findByOrderId(ORDER_ID, pageable)).thenReturn(page);

            Page<DeliveryNoteResponse> result = service.listByOrder(ORDER_ID, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).orderId()).isEqualTo(ORDER_ID);
        }
    }
}
