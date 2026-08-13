package com.distribuidora.notification.controller;

import com.distribuidora.notification.domain.NotificationChannel;
import com.distribuidora.notification.domain.NotificationStatus;
import com.distribuidora.notification.domain.NotificationTrigger;
import com.distribuidora.notification.dto.NotificationLogResponse;
import com.distribuidora.notification.dto.NotificationStatsResponse;
import com.distribuidora.notification.service.AdminNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNotificationControllerTest {

    @Mock
    private AdminNotificationService adminNotificationService;

    @InjectMocks
    private AdminNotificationController adminNotificationController;

    private static final UUID LOG_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private NotificationLogResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = NotificationLogResponse.builder()
                .id(LOG_ID)
                .orderId(ORDER_ID)
                .recipientId(UUID.randomUUID())
                .recipientAddress("+5491145678900")
                .channel(NotificationChannel.WHATSAPP)
                .trigger(NotificationTrigger.ORDER_STOCK_CREATED)
                .status(NotificationStatus.SENT)
                .attempts(1)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getNotificationsReturnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<NotificationLogResponse> page = new PageImpl<>(List.of(sampleResponse), pageable, 1);

        when(adminNotificationService.getNotifications(eq(NotificationStatus.SENT), eq(NotificationChannel.WHATSAPP), eq(ORDER_ID), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<NotificationLogResponse>> response = adminNotificationController.getNotifications(NotificationStatus.SENT, NotificationChannel.WHATSAPP, ORDER_ID, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void getNotificationByIdReturnsLog() {
        when(adminNotificationService.getNotificationById(LOG_ID)).thenReturn(sampleResponse);

        ResponseEntity<NotificationLogResponse> response = adminNotificationController.getNotificationById(LOG_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(LOG_ID);
    }

    @Test
    void retryNotificationReturnsUpdatedLog() {
        when(adminNotificationService.retryNotification(LOG_ID)).thenReturn(sampleResponse);

        ResponseEntity<NotificationLogResponse> response = adminNotificationController.retryNotification(LOG_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(adminNotificationService).retryNotification(LOG_ID);
    }

    @Test
    void getStatsReturnsStatsResponse() {
        NotificationStatsResponse stats = NotificationStatsResponse.builder()
                .totalCount(10)
                .sentCount(8)
                .failedCount(2)
                .build();

        when(adminNotificationService.getStats()).thenReturn(stats);

        ResponseEntity<NotificationStatsResponse> response = adminNotificationController.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalCount()).isEqualTo(10);
    }
}
