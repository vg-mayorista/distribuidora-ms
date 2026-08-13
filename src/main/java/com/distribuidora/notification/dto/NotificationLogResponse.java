package com.distribuidora.notification.dto;

import com.distribuidora.notification.domain.NotificationChannel;
import com.distribuidora.notification.domain.NotificationLog;
import com.distribuidora.notification.domain.NotificationStatus;
import com.distribuidora.notification.domain.NotificationTrigger;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class NotificationLogResponse {

    private final UUID id;
    private final UUID orderId;
    private final UUID recipientId;
    private final String recipientAddress;
    private final NotificationChannel channel;
    private final NotificationTrigger trigger;
    private final NotificationStatus status;
    private final String errorDetails;
    private final Integer attempts;
    private final Instant createdAt;
    private final Instant sentAt;
    private final Instant updatedAt;

    public static NotificationLogResponse fromEntity(NotificationLog log) {
        if (log == null) {
            return null;
        }

        return NotificationLogResponse.builder()
                .id(log.getId())
                .orderId(log.getOrderId())
                .recipientId(log.getRecipientId())
                .recipientAddress(log.getRecipientAddress())
                .channel(log.getChannel())
                .trigger(log.getTrigger())
                .status(log.getStatus())
                .errorDetails(log.getErrorDetails())
                .attempts(log.getAttempts())
                .createdAt(log.getCreatedAt())
                .sentAt(log.getSentAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}
