package com.distribuidora.notification.sender;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSendResult {
    private final boolean success;
    private final String externalId;
    private final String errorMessage;

    public static NotificationSendResult success(String externalId) {
        return NotificationSendResult.builder()
                .success(true)
                .externalId(externalId)
                .build();
    }

    public static NotificationSendResult failure(String errorMessage) {
        return NotificationSendResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
