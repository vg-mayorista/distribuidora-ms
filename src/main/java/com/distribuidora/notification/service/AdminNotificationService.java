package com.distribuidora.notification.service;

import com.distribuidora.exception.NotificationLogNotFoundException;
import com.distribuidora.notification.domain.NotificationChannel;
import com.distribuidora.notification.domain.NotificationLog;
import com.distribuidora.notification.domain.NotificationStatus;
import com.distribuidora.notification.dto.NotificationLogResponse;
import com.distribuidora.notification.dto.NotificationStatsResponse;
import com.distribuidora.notification.repository.NotificationLogRepository;
import com.distribuidora.notification.sender.EmailSender;
import com.distribuidora.notification.sender.NotificationSendResult;
import com.distribuidora.notification.sender.WhatsAppSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final WhatsAppSender whatsAppSender;
    private final EmailSender emailSender;

    @Transactional(readOnly = true)
    public Page<NotificationLogResponse> getNotifications(NotificationStatus status, NotificationChannel channel, UUID orderId, Pageable pageable) {
        return notificationLogRepository.findByFilters(status, channel, orderId, pageable)
                .map(NotificationLogResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public NotificationLogResponse getNotificationById(UUID id) {
        NotificationLog notificationLog = notificationLogRepository.findById(id)
                .orElseThrow(() -> new NotificationLogNotFoundException(id));
        return NotificationLogResponse.fromEntity(notificationLog);
    }

    @Transactional
    public NotificationLogResponse retryNotification(UUID id) {
        NotificationLog logEntry = notificationLogRepository.findById(id)
                .orElseThrow(() -> new NotificationLogNotFoundException(id));

        logEntry.setAttempts((logEntry.getAttempts() != null ? logEntry.getAttempts() : 0) + 1);

        try {
            if (logEntry.getChannel() == NotificationChannel.WHATSAPP) {
                String message = "📦 *Reintento de Notificación Pedido Express (STOCK)*\n" +
                        "• *ID Pedido:* " + logEntry.getOrderId();
                NotificationSendResult result = whatsAppSender.sendWhatsApp(logEntry.getRecipientAddress(), message);
                if (result.isSuccess()) {
                    logEntry.setStatus(NotificationStatus.SENT);
                    logEntry.setErrorDetails(null);
                    logEntry.setSentAt(Instant.now());
                } else {
                    logEntry.setStatus(NotificationStatus.FAILED);
                    logEntry.setErrorDetails(result.getErrorMessage());
                }
            } else if (logEntry.getChannel() == NotificationChannel.EMAIL) {
                String subject = "📦 Reintento de Notificación Pedido #" + logEntry.getOrderId().toString().substring(0, 8);
                String body = "<h2>📦 Reintento de Notificación Pedido Express</h2><p><strong>ID Pedido:</strong> " + logEntry.getOrderId() + "</p>";
                NotificationSendResult result = emailSender.sendEmail(logEntry.getRecipientAddress(), subject, body);
                if (result.isSuccess()) {
                    logEntry.setStatus(NotificationStatus.SENT);
                    logEntry.setErrorDetails(null);
                    logEntry.setSentAt(Instant.now());
                } else {
                    logEntry.setStatus(NotificationStatus.FAILED);
                    logEntry.setErrorDetails(result.getErrorMessage());
                }
            }
        } catch (Exception ex) {
            log.error("[AdminNotificationService] Error re-sending notification {}: {}", id, ex.getMessage());
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorDetails(ex.getMessage());
        }

        NotificationLog saved = notificationLogRepository.save(logEntry);
        return NotificationLogResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public NotificationStatsResponse getStats() {
        return NotificationStatsResponse.builder()
                .totalCount(notificationLogRepository.count())
                .sentCount(notificationLogRepository.countByStatus(NotificationStatus.SENT))
                .failedCount(notificationLogRepository.countByStatus(NotificationStatus.FAILED))
                .skippedCount(notificationLogRepository.countByStatus(NotificationStatus.SKIPPED))
                .pendingCount(notificationLogRepository.countByStatus(NotificationStatus.PENDING))
                .whatsappCount(notificationLogRepository.countByChannel(NotificationChannel.WHATSAPP))
                .emailCount(notificationLogRepository.countByChannel(NotificationChannel.EMAIL))
                .build();
    }
}
