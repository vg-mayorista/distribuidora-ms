package com.distribuidora.notification.repository;

import com.distribuidora.notification.domain.NotificationChannel;
import com.distribuidora.notification.domain.NotificationLog;
import com.distribuidora.notification.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByOrderId(UUID orderId);

    Page<NotificationLog> findByStatus(NotificationStatus status, Pageable pageable);

    Page<NotificationLog> findByChannel(NotificationChannel channel, Pageable pageable);

    Page<NotificationLog> findByStatusAndChannel(NotificationStatus status, NotificationChannel channel, Pageable pageable);
}
