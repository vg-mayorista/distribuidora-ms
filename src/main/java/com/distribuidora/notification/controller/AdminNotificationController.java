package com.distribuidora.notification.controller;

import com.distribuidora.notification.domain.NotificationChannel;
import com.distribuidora.notification.domain.NotificationStatus;
import com.distribuidora.notification.dto.NotificationLogResponse;
import com.distribuidora.notification.dto.NotificationStatsResponse;
import com.distribuidora.notification.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationLogResponse>> getNotifications(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) UUID orderId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificationLogResponse> notifications = adminNotificationService.getNotifications(status, channel, orderId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationLogResponse> getNotificationById(@PathVariable UUID id) {
        NotificationLogResponse response = adminNotificationService.getNotificationById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<NotificationLogResponse> retryNotification(@PathVariable UUID id) {
        NotificationLogResponse response = adminNotificationService.retryNotification(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsResponse> getStats() {
        NotificationStatsResponse stats = adminNotificationService.getStats();
        return ResponseEntity.ok(stats);
    }
}
