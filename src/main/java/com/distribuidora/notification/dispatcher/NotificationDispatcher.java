package com.distribuidora.notification.dispatcher;

import com.distribuidora.model.BusinessConfig;
import com.distribuidora.model.User;
import com.distribuidora.notification.domain.NotificationChannel;
import com.distribuidora.notification.domain.NotificationLog;
import com.distribuidora.notification.domain.NotificationStatus;
import com.distribuidora.notification.domain.NotificationTrigger;
import com.distribuidora.notification.event.OrderNotificationEvent;
import com.distribuidora.notification.recipient.NotificationRecipientResolver;
import com.distribuidora.notification.repository.NotificationLogRepository;
import com.distribuidora.notification.sender.EmailSender;
import com.distribuidora.notification.sender.NotificationSendResult;
import com.distribuidora.notification.sender.WhatsAppSender;
import com.distribuidora.notification.util.ArgentinePhoneNormalizer;
import com.distribuidora.service.BusinessConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final BusinessConfigService businessConfigService;
    private final NotificationRecipientResolver recipientResolver;
    private final WhatsAppSender whatsAppSender;
    private final EmailSender emailSender;
    private final NotificationLogRepository notificationLogRepository;

    public void dispatchOrderCreatedNotification(OrderNotificationEvent event) {
        BusinessConfig config = businessConfigService.getOrInitConfig();

        if (!Boolean.TRUE.equals(config.getNotifyOnStockOrderCreated())) {
            log.info("[NotificationDispatcher] Stock order notifications disabled in BusinessConfig");
            return;
        }

        List<User> recipients = recipientResolver.resolveRecipients();
        if (recipients.isEmpty()) {
            log.info("[NotificationDispatcher] No active recipients found for order {}", event.getOrderId());
            return;
        }

        String waBody = buildWhatsAppBody(event);
        String emailSubject = "📦 Nuevo Pedido Express #" + event.getOrderId().toString().substring(0, 8) + " - VG Mayorista";
        String emailHtml = buildEmailBody(event);

        Set<String> processedPhones = new HashSet<>();
        Set<String> processedEmails = new HashSet<>();

        for (User recipient : recipients) {
            dispatchWhatsApp(config, recipient, event, waBody, processedPhones);
            dispatchEmail(config, recipient, event, emailSubject, emailHtml, processedEmails);
        }
    }

    private void dispatchWhatsApp(BusinessConfig config, User recipient, OrderNotificationEvent event, String message, Set<String> processedPhones) {
        if (!Boolean.TRUE.equals(config.getNotifyWhatsappEnabled())) {
            saveLog(event, recipient, NotificationChannel.WHATSAPP, recipient.getPhone(), NotificationStatus.SKIPPED, "Canal WhatsApp deshabilitado en BusinessConfig", 0);
            return;
        }

        Optional<String> phoneOpt = ArgentinePhoneNormalizer.normalize(recipient.getPhone());
        if (phoneOpt.isEmpty()) {
            saveLog(event, recipient, NotificationChannel.WHATSAPP, recipient.getPhone(), NotificationStatus.SKIPPED, "Número de celular no disponible o inválido para WhatsApp", 0);
            return;
        }

        String phone = phoneOpt.get();
        if (processedPhones.contains(phone)) {
            log.info("[NotificationDispatcher] Skipping duplicate WhatsApp phone address {} for order {}", phone, event.getOrderId());
            return;
        }
        processedPhones.add(phone);

        try {
            NotificationSendResult result = whatsAppSender.sendWhatsApp(phone, message);
            if (result.isSuccess()) {
                saveLog(event, recipient, NotificationChannel.WHATSAPP, phone, NotificationStatus.SENT, null, 1);
            } else {
                saveLog(event, recipient, NotificationChannel.WHATSAPP, phone, NotificationStatus.FAILED, result.getErrorMessage(), 1);
            }
        } catch (Exception ex) {
            log.error("[NotificationDispatcher] WhatsApp dispatch failed for recipient {}: {}", recipient.getId(), ex.getMessage());
            saveLog(event, recipient, NotificationChannel.WHATSAPP, phone, NotificationStatus.FAILED, ex.getMessage(), 3);
        }
    }

    private void dispatchEmail(BusinessConfig config, User recipient, OrderNotificationEvent event, String subject, String bodyHtml, Set<String> processedEmails) {
        if (!Boolean.TRUE.equals(config.getNotifyEmailEnabled())) {
            saveLog(event, recipient, NotificationChannel.EMAIL, recipient.getEmail(), NotificationStatus.SKIPPED, "Canal Email deshabilitado en BusinessConfig", 0);
            return;
        }

        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            saveLog(event, recipient, NotificationChannel.EMAIL, recipient.getEmail(), NotificationStatus.SKIPPED, "Email del usuario no disponible", 0);
            return;
        }

        String email = recipient.getEmail().trim().toLowerCase();
        if (processedEmails.contains(email)) {
            log.info("[NotificationDispatcher] Skipping duplicate email address {} for order {}", email, event.getOrderId());
            return;
        }
        processedEmails.add(email);

        try {
            NotificationSendResult result = emailSender.sendEmail(recipient.getEmail(), subject, bodyHtml);
            if (result.isSuccess()) {
                saveLog(event, recipient, NotificationChannel.EMAIL, recipient.getEmail(), NotificationStatus.SENT, null, 1);
            } else {
                saveLog(event, recipient, NotificationChannel.EMAIL, recipient.getEmail(), NotificationStatus.FAILED, result.getErrorMessage(), 1);
            }
        } catch (Exception ex) {
            log.error("[NotificationDispatcher] Email dispatch failed for recipient {}: {}", recipient.getId(), ex.getMessage());
            saveLog(event, recipient, NotificationChannel.EMAIL, recipient.getEmail(), NotificationStatus.FAILED, ex.getMessage(), 3);
        }
    }

    private void saveLog(OrderNotificationEvent event, User recipient, NotificationChannel channel, String address, NotificationStatus status, String errorDetails, int attempts) {
        NotificationLog logEntry = NotificationLog.builder()
                .orderId(event.getOrderId())
                .recipientId(recipient != null ? recipient.getId() : null)
                .recipientAddress(address != null ? address : "N/A")
                .channel(channel)
                .trigger(NotificationTrigger.ORDER_STOCK_CREATED)
                .status(status)
                .errorDetails(errorDetails)
                .attempts(attempts)
                .build();

        notificationLogRepository.save(logEntry);
    }

    private String buildWhatsAppBody(OrderNotificationEvent event) {
        return "📦 *Nuevo Pedido Express (STOCK)*\n" +
                "• *Cliente:* " + event.getCustomerName() + "\n" +
                "• *Teléfono:* " + (event.getCustomerPhone() != null ? event.getCustomerPhone() : "N/A") + "\n" +
                "• *Dirección:* " + (event.getDeliveryAddress() != null ? event.getDeliveryAddress() : "N/A") + "\n" +
                "• *Items:* " + event.getItemsCount() + "\n" +
                "• *Total:* $" + event.getTotal();
    }

    private String buildEmailBody(OrderNotificationEvent event) {
        return "<h2>📦 Nuevo Pedido Express (STOCK)</h2>" +
                "<p><strong>ID Pedido:</strong> " + event.getOrderId() + "</p>" +
                "<p><strong>Cliente:</strong> " + event.getCustomerName() + "</p>" +
                "<p><strong>Teléfono:</strong> " + (event.getCustomerPhone() != null ? event.getCustomerPhone() : "N/A") + "</p>" +
                "<p><strong>Dirección:</strong> " + (event.getDeliveryAddress() != null ? event.getDeliveryAddress() : "N/A") + "</p>" +
                "<p><strong>Cantidad de Items:</strong> " + event.getItemsCount() + "</p>" +
                "<p><strong>Monto Total:</strong> $" + event.getTotal() + "</p>";
    }
}
