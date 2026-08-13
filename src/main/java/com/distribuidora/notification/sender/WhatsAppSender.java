package com.distribuidora.notification.sender;

import com.distribuidora.notification.config.NotificationProperties;
import com.distribuidora.notification.util.ArgentinePhoneNormalizer;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppSender {

    private final NotificationProperties notificationProperties;

    @Retryable(
            retryFor = { Exception.class },
            maxAttemptsExpression = "${notify.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${notify.retry.initial-backoff-ms:2000}",
                    multiplier = 2.0
            )
    )
    public NotificationSendResult sendWhatsApp(String rawPhone, String messageBody) {
        Optional<String> normalizedOpt = ArgentinePhoneNormalizer.normalize(rawPhone);
        if (normalizedOpt.isEmpty()) {
            log.warn("[WhatsAppSender] Invalid or missing phone number: '{}'", rawPhone);
            return NotificationSendResult.failure("Teléfono no disponible o inválido para WhatsApp: " + rawPhone);
        }

        String normalizedPhone = normalizedOpt.get();
        NotificationProperties.Twilio twilioProps = notificationProperties.getTwilio();

        String accountSid = twilioProps.getAccountSid();
        if (accountSid == null || accountSid.isBlank()) {
            accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        }

        String authToken = twilioProps.getAuthToken();
        if (authToken == null || authToken.isBlank()) {
            authToken = System.getenv("TWILIO_AUTH_TOKEN");
        }

        String fromNumber = twilioProps.getWhatsappFrom();
        if (fromNumber == null || fromNumber.isBlank()) {
            fromNumber = System.getenv("TWILIO_WHATSAPP_FROM");
        }
        if (fromNumber == null || fromNumber.isBlank()) {
            fromNumber = "whatsapp:+14155238886";
        }

        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            log.info("[WhatsAppSender Mock Dry-Run] Credenciales de Twilio no detectadas (accountSidPresent={}, authTokenPresent={}). To: whatsapp:{}, Body: {}",
                    (accountSid != null && !accountSid.isBlank()),
                    (authToken != null && !authToken.isBlank()),
                    normalizedPhone, messageBody);
            String mockSid = "MOCK_WA_" + UUID.randomUUID().toString().substring(0, 8);
            return NotificationSendResult.success(mockSid);
        }

        try {
            Twilio.init(accountSid, authToken);
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + normalizedPhone),
                    new PhoneNumber(fromNumber),
                    messageBody
            ).create();

            log.info("[WhatsAppSender] Successfully sent WhatsApp message SID: {} to {}", message.getSid(), normalizedPhone);
            return NotificationSendResult.success(message.getSid());
        } catch (Exception e) {
            log.error("[WhatsAppSender] Failed to send WhatsApp message to {}: {}", normalizedPhone, e.getMessage());
            throw e; // Re-throw to trigger @Retryable
        }
    }
}
