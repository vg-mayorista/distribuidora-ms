package com.distribuidora.notification.service;

import com.distribuidora.exception.NotificationLogNotFoundException;
import com.distribuidora.notification.domain.NotificationChannel;
import com.distribuidora.notification.domain.NotificationLog;
import com.distribuidora.notification.domain.NotificationStatus;
import com.distribuidora.notification.domain.NotificationTrigger;
import com.distribuidora.notification.dto.NotificationLogResponse;
import com.distribuidora.notification.dto.NotificationStatsResponse;
import com.distribuidora.notification.repository.NotificationLogRepository;
import com.distribuidora.notification.sender.EmailSender;
import com.distribuidora.notification.sender.NotificationSendResult;
import com.distribuidora.notification.sender.WhatsAppSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private WhatsAppSender whatsAppSender;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private AdminNotificationService adminNotificationService;

    private static final UUID LOG_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private NotificationLog sampleLog(NotificationChannel channel, NotificationStatus status) {
        return NotificationLog.builder()
                .id(LOG_ID)
                .orderId(ORDER_ID)
                .recipientId(UUID.randomUUID())
                .recipientAddress(channel == NotificationChannel.WHATSAPP ? "+5491145678900" : "test@test.com")
                .channel(channel)
                .trigger(NotificationTrigger.ORDER_STOCK_CREATED)
                .status(status)
                .attempts(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    class GetNotifications {
        @Test
        void returnsFilteredPage() {
            Pageable pageable = PageRequest.of(0, 10);
            NotificationLog logEntry = sampleLog(NotificationChannel.WHATSAPP, NotificationStatus.SENT);
            Page<NotificationLog> page = new PageImpl<>(List.of(logEntry), pageable, 1);

            when(notificationLogRepository.findByFilters(NotificationStatus.SENT, NotificationChannel.WHATSAPP, ORDER_ID, pageable))
                    .thenReturn(page);

            Page<NotificationLogResponse> result = adminNotificationService.getNotifications(NotificationStatus.SENT, NotificationChannel.WHATSAPP, ORDER_ID, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(LOG_ID);
        }
    }

    @Nested
    class GetNotificationById {
        @Test
        void returnsLogResponseWhenFound() {
            NotificationLog logEntry = sampleLog(NotificationChannel.WHATSAPP, NotificationStatus.SENT);
            when(notificationLogRepository.findById(LOG_ID)).thenReturn(Optional.of(logEntry));

            NotificationLogResponse result = adminNotificationService.getNotificationById(LOG_ID);

            assertThat(result.getId()).isEqualTo(LOG_ID);
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(notificationLogRepository.findById(LOG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminNotificationService.getNotificationById(LOG_ID))
                    .isInstanceOf(NotificationLogNotFoundException.class);
        }
    }

    @Nested
    class RetryNotification {
        @Test
        void retriesWhatsAppNotificationSuccessfully() {
            NotificationLog logEntry = sampleLog(NotificationChannel.WHATSAPP, NotificationStatus.FAILED);
            when(notificationLogRepository.findById(LOG_ID)).thenReturn(Optional.of(logEntry));
            when(whatsAppSender.sendWhatsApp(anyString(), anyString())).thenReturn(NotificationSendResult.success("WA_RETRY_123"));
            when(notificationLogRepository.save(any(NotificationLog.class))).thenAnswer(i -> i.getArgument(0));

            NotificationLogResponse result = adminNotificationService.retryNotification(LOG_ID);

            assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(result.getAttempts()).isEqualTo(2);
            verify(whatsAppSender).sendWhatsApp(anyString(), anyString());
        }

        @Test
        void retriesEmailNotificationSuccessfully() {
            NotificationLog logEntry = sampleLog(NotificationChannel.EMAIL, NotificationStatus.FAILED);
            when(notificationLogRepository.findById(LOG_ID)).thenReturn(Optional.of(logEntry));
            when(emailSender.sendEmail(anyString(), anyString(), anyString())).thenReturn(NotificationSendResult.success("MAIL_RETRY_123"));
            when(notificationLogRepository.save(any(NotificationLog.class))).thenAnswer(i -> i.getArgument(0));

            NotificationLogResponse result = adminNotificationService.retryNotification(LOG_ID);

            assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(result.getAttempts()).isEqualTo(2);
            verify(emailSender).sendEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    class GetStats {
        @Test
        void returnsAggregatedStats() {
            when(notificationLogRepository.count()).thenReturn(10L);
            when(notificationLogRepository.countByStatus(NotificationStatus.SENT)).thenReturn(7L);
            when(notificationLogRepository.countByStatus(NotificationStatus.FAILED)).thenReturn(2L);
            when(notificationLogRepository.countByStatus(NotificationStatus.SKIPPED)).thenReturn(1L);
            when(notificationLogRepository.countByStatus(NotificationStatus.PENDING)).thenReturn(0L);
            when(notificationLogRepository.countByChannel(NotificationChannel.WHATSAPP)).thenReturn(5L);
            when(notificationLogRepository.countByChannel(NotificationChannel.EMAIL)).thenReturn(5L);

            NotificationStatsResponse stats = adminNotificationService.getStats();

            assertThat(stats.getTotalCount()).isEqualTo(10L);
            assertThat(stats.getSentCount()).isEqualTo(7L);
            assertThat(stats.getFailedCount()).isEqualTo(2L);
            assertThat(stats.getWhatsappCount()).isEqualTo(5L);
        }
    }
}
