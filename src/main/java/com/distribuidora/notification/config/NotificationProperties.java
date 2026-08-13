package com.distribuidora.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "notify")
@Getter
@Setter
public class NotificationProperties {

    private Whatsapp whatsapp = new Whatsapp();
    private Email email = new Email();
    private Retry retry = new Retry();
    private Twilio twilio = new Twilio();

    @Getter
    @Setter
    public static class Whatsapp {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Email {
        private boolean enabled = true;
        private String fromAddress = "noreply@distribuidora.com";
        private String fromName = "VG Mayorista";
    }

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
        private long initialBackoffMs = 2000;
    }

    @Getter
    @Setter
    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String whatsappFrom = "whatsapp:+14155238886";
    }
}
