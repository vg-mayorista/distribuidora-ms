package com.distribuidora.notification.dispatcher;

import com.distribuidora.model.BusinessConfig;
import com.distribuidora.model.OrderType;
import com.distribuidora.model.User;
import com.distribuidora.notification.domain.NotificationChannel;
import com.distribuidora.notification.domain.NotificationLog;
import com.distribuidora.notification.domain.NotificationStatus;
import com.distribuidora.notification.event.OrderNotificationEvent;
import com.distribuidora.notification.recipient.NotificationRecipientResolver;
import com.distribuidora.notification.repository.NotificationLogRepository;
import com.distribuidora.notification.sender.EmailSender;
import com.distribuidora.notification.sender.NotificationSendResult;
import com.distribuidora.notification.sender.WhatsAppSender;
import com.distribuidora.service.BusinessConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private BusinessConfigService businessConfigService;

    @Mock
    private NotificationRecipientResolver recipientResolver;

    @Mock
    private WhatsAppSender whatsAppSender;

    @Mock
    private EmailSender emailSender;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private NotificationDispatcher notificationDispatcher;

    private OrderNotificationEvent sampleEvent;
    private BusinessConfig defaultConfig;

    @BeforeEach
    void setUp() {
        sampleEvent = OrderNotificationEvent.builder()
                .orderId(UUID.randomUUID())
                .orderType(OrderType.STOCK)
                .customerName("Juan Perez")
                .customerPhone("1145678900")
                .deliveryAddress("Av. Corrientes 1234")
                .subtotal(new BigDecimal("10000.00"))
                .total(new BigDecimal("10000.00"))
                .itemsCount(5)
                .createdAt(Instant.now())
                .build();

        defaultConfig = BusinessConfig.builder()
                .notifyOnStockOrderCreated(true)
                .notifyWhatsappEnabled(true)
                .notifyEmailEnabled(true)
                .build();
    }

    @Test
    void doesNothingWhenStockNotificationsDisabled() {
        defaultConfig.setNotifyOnStockOrderCreated(false);
        when(businessConfigService.getOrInitConfig()).thenReturn(defaultConfig);

        notificationDispatcher.dispatchOrderCreatedNotification(sampleEvent);

        verify(recipientResolver, never()).resolveRecipients();
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void dispatchesSuccessfullyForValidRecipient() {
        User recipient = User.builder()
                .id(UUID.randomUUID())
                .firstName("Admin")
                .lastName("User")
                .email("admin@test.com")
                .phone("1145678900")
                .build();

        when(businessConfigService.getOrInitConfig()).thenReturn(defaultConfig);
        when(recipientResolver.resolveRecipients()).thenReturn(List.of(recipient));
        when(whatsAppSender.sendWhatsApp(anyString(), anyString())).thenReturn(NotificationSendResult.success("WA_123"));
        when(emailSender.sendEmail(anyString(), anyString(), anyString())).thenReturn(NotificationSendResult.success("MAIL_123"));

        notificationDispatcher.dispatchOrderCreatedNotification(sampleEvent);

        verify(whatsAppSender).sendWhatsApp(anyString(), anyString());
        verify(emailSender).sendEmail(anyString(), anyString(), anyString());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(2)).save(logCaptor.capture());

        List<NotificationLog> logs = logCaptor.getAllValues();
        assertThat(logs).hasSize(2);

        NotificationLog waLog = logs.stream().filter(l -> l.getChannel() == NotificationChannel.WHATSAPP).findFirst().orElseThrow();
        assertThat(waLog.getStatus()).isEqualTo(NotificationStatus.SENT);

        NotificationLog emailLog = logs.stream().filter(l -> l.getChannel() == NotificationChannel.EMAIL).findFirst().orElseThrow();
        assertThat(emailLog.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void handlesMissingUserDataWithSkippedStatus() {
        User recipientNoData = User.builder()
                .id(UUID.randomUUID())
                .firstName("NoData")
                .lastName("User")
                .email(null)
                .phone(null)
                .build();

        when(businessConfigService.getOrInitConfig()).thenReturn(defaultConfig);
        when(recipientResolver.resolveRecipients()).thenReturn(List.of(recipientNoData));

        notificationDispatcher.dispatchOrderCreatedNotification(sampleEvent);

        verify(whatsAppSender, never()).sendWhatsApp(anyString(), anyString());
        verify(emailSender, never()).sendEmail(anyString(), anyString(), anyString());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(2)).save(logCaptor.capture());

        List<NotificationLog> logs = logCaptor.getAllValues();
        assertThat(logs).extracting(NotificationLog::getStatus).containsOnly(NotificationStatus.SKIPPED);
    }

    @Test
    void handlesSenderExceptionWithFailedStatus() {
        User recipient = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .phone("1145678900")
                .build();

        when(businessConfigService.getOrInitConfig()).thenReturn(defaultConfig);
        when(recipientResolver.resolveRecipients()).thenReturn(List.of(recipient));
        when(whatsAppSender.sendWhatsApp(anyString(), anyString())).thenThrow(new RuntimeException("Twilio API Error"));
        when(emailSender.sendEmail(anyString(), anyString(), anyString())).thenReturn(NotificationSendResult.success("MAIL_123"));

        notificationDispatcher.dispatchOrderCreatedNotification(sampleEvent);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(2)).save(logCaptor.capture());

        List<NotificationLog> logs = logCaptor.getAllValues();
        NotificationLog waLog = logs.stream().filter(l -> l.getChannel() == NotificationChannel.WHATSAPP).findFirst().orElseThrow();
        assertThat(waLog.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(waLog.getErrorDetails()).contains("Twilio API Error");
    }
}
