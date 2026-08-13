package com.distribuidora.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationStatsResponse {

    private final long totalCount;
    private final long sentCount;
    private final long failedCount;
    private final long skippedCount;
    private final long pendingCount;

    private final long whatsappCount;
    private final long emailCount;
}
