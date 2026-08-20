package com.distribuidora.deliverynote.service;

import com.distribuidora.deliverynote.exception.DeliveryNoteDownloadNotAvailableException;
import com.distribuidora.deliverynote.exception.DeliveryNoteNotFoundException;
import com.distribuidora.deliverynote.model.DeliveryNote;
import com.distribuidora.deliverynote.model.DeliveryNoteItem;
import com.distribuidora.deliverynote.model.DeliveryNoteStatus;
import com.distribuidora.deliverynote.repository.DeliveryNoteRepository;
import com.distribuidora.model.Order;
import com.distribuidora.model.OrderItem;
import com.distribuidora.model.OrderStatus;
import com.distribuidora.model.OrderType;
import com.distribuidora.model.User;
import com.distribuidora.repository.OrderRepository;
import com.distribuidora.repository.UserRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryNoteDocxServiceTest {

    private static final ZoneId ARG = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDate.of(2026, 8, 19).atTime(10, 0).atZone(ARG).toInstant(),
            ARG);

    @Mock
    DeliveryNoteRepository deliveryNoteRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    DeliveryNoteDocxService service;

    private static final UUID DN_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @BeforeEach
    void injectClock() throws Exception {
        Field field = DeliveryNoteDocxService.class.getDeclaredField("clock");
        field.setAccessible(true);
        field.set(service, FIXED_CLOCK);
    }

    private DeliveryNote baseDeliveryNote() {
        return DeliveryNote.builder()
                .id(DN_ID)
                .orderId(ORDER_ID)
                .deliveryNoteNumber("R-2026-0001")
                .status(DeliveryNoteStatus.GENERATED)
                .issueDate(LocalDate.of(2026, 8, 20))
                .deliveryDate(LocalDate.of(2026, 8, 25))
                .notes("Nota de prueba")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .items(List.of(
                        DeliveryNoteItem.builder()
                                .id(UUID.randomUUID())
                                .productId(PRODUCT_ID)
                                .productName("Yerba Mate")
                                .unitPrice(new BigDecimal("1500.00"))
                                .quantityDelivered(2)
                                .build()
                ))
                .build();
    }

    private Order baseOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .status(OrderStatus.ENVIADO)
                .type(OrderType.WHOLESALE)
                .deliveryDate(LocalDate.of(2026, 8, 25))
                .items(List.of(
                        OrderItem.builder()
                                .productId(PRODUCT_ID)
                                .productName("Yerba Mate")
                                .unitPrice(new BigDecimal("1500.00"))
                                .quantity(2)
                                .subtotal(new BigDecimal("3000.00"))
                                .build()
                ))
                .build();
    }

    private User baseUser() {
        return User.builder()
                .id(USER_ID)
                .firstName("Juan")
                .lastName("Perez")
                .zone("Centro")
                .build();
    }

    private List<String> getAllParagraphTexts(byte[] docx) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.toList());
        }
    }

    private List<String> getAllCellTexts(byte[] docx) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return document.getTables().stream()
                    .flatMap(t -> t.getRows().stream())
                    .flatMap(r -> r.getTableCells().stream())
                    .flatMap(c -> c.getParagraphs().stream())
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.toList());
        }
    }

    @Test
    void generatesValidDocxWithFilledFields() throws Exception {
        DeliveryNote dn = baseDeliveryNote();
        Order order = baseOrder();
        User user = baseUser();

        when(deliveryNoteRepository.findById(DN_ID)).thenReturn(Optional.of(dn));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        byte[] docx = service.generate(DN_ID);

        assertThat(docx).isNotEmpty();
        assertThat(docx[0]).isEqualTo((byte) 0x50);
        assertThat(docx[1]).isEqualTo((byte) 0x4B);

        List<String> paragraphs = getAllParagraphTexts(docx);
        assertThat(paragraphs).anySatisfy(text -> assertThat(text).contains("Juan Perez"));
        assertThat(paragraphs).anySatisfy(text -> assertThat(text).contains("Centro"));
        assertThat(paragraphs).anySatisfy(text -> assertThat(text).contains("R-2026-0001"));
        assertThat(paragraphs).anySatisfy(text -> assertThat(text).contains("25/08/2026"));
        assertThat(paragraphs).anySatisfy(text -> assertThat(text).contains("Nota de prueba"));

        List<String> cells = getAllCellTexts(docx);
        assertThat(cells).anySatisfy(text -> assertThat(text).contains("Yerba Mate"));
        assertThat(cells).anySatisfy(text -> assertThat(text).contains("1500.00"));
        assertThat(cells).anySatisfy(text -> assertThat(text).contains("3000.00"));
    }

    @Test
    void throwsWhenDeliveryNoteNotFound() {
        when(deliveryNoteRepository.findById(DN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(DN_ID))
                .isInstanceOf(DeliveryNoteNotFoundException.class);

        verify(orderRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void usesClienteFallbackWhenUserNotFound() throws Exception {
        DeliveryNote dn = baseDeliveryNote();
        Order order = baseOrder();

        when(deliveryNoteRepository.findById(DN_ID)).thenReturn(Optional.of(dn));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        byte[] docx = service.generate(DN_ID);
        List<String> paragraphs = getAllParagraphTexts(docx);
        assertThat(paragraphs).anySatisfy(text -> assertThat(text).contains("Cliente " + USER_ID.toString()));
    }

    @Test
    void blocksDownloadBeforeCutoffOnTuesday() throws Exception {
        DeliveryNote dn = baseDeliveryNote();
        dn.setIssueDate(LocalDate.of(2026, 8, 18));

        when(deliveryNoteRepository.findById(DN_ID)).thenReturn(Optional.of(dn));

        Field field = DeliveryNoteDocxService.class.getDeclaredField("clock");
        field.setAccessible(true);
        Clock tuesdayBeforeCutoff = Clock.fixed(
                LocalDate.of(2026, 8, 18).atTime(17, 30).atZone(ARG).toInstant(),
                ARG);
        field.set(service, tuesdayBeforeCutoff);

        assertThatThrownBy(() -> service.generate(DN_ID))
                .isInstanceOf(DeliveryNoteDownloadNotAvailableException.class);
    }
}
