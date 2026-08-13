package com.distribuidora.notification.sender;

import com.distribuidora.notification.config.NotificationProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final NotificationProperties notificationProperties;

    @Retryable(
            retryFor = { Exception.class },
            maxAttemptsExpression = "${notify.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${notify.retry.initial-backoff-ms:2000}",
                    multiplier = 2.0
            )
    )
    public NotificationSendResult sendEmail(String recipientEmail, String subject, String bodyHtml) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[EmailSender] Invalid or missing email address: '{}'", recipientEmail);
            return NotificationSendResult.failure("Email no disponible o inválido");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        NotificationProperties.Email emailProps = notificationProperties.getEmail();

        if (mailSender == null) {
            log.info("[EmailSender Mock Dry-Run] To: {}, Subject: {}", recipientEmail, subject);
            String mockId = "MOCK_MAIL_" + UUID.randomUUID().toString().substring(0, 8);
            return NotificationSendResult.success(mockId);
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emailProps.getFromAddress(), emailProps.getFromName());
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true);

            mailSender.send(mimeMessage);
            log.info("[EmailSender] Successfully sent email to {} with subject '{}'", recipientEmail, subject);
            return NotificationSendResult.success(UUID.randomUUID().toString());
        } catch (Exception e) {
            log.error("[EmailSender] Failed to send email to {}: {}", recipientEmail, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }
}
