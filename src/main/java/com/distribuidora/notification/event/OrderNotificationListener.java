package com.distribuidora.notification.event;

import com.distribuidora.model.User;
import com.distribuidora.notification.recipient.NotificationRecipientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final NotificationRecipientResolver recipientResolver;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderNotification(OrderNotificationEvent event) {
        log.info("[notification] OrderNotificationEvent received post-commit for order {} ({})",
                event.getOrderId(), event.getOrderType());

        List<User> recipients = recipientResolver.resolveRecipients();
        log.info("[notification] Resolved {} active recipient(s) for order {}",
                recipients.size(), event.getOrderId());

        for (User recipient : recipients) {
            log.debug("[notification] Recipient target: {} {} (email={}, phone={})",
                    recipient.getFirstName(), recipient.getLastName(),
                    recipient.getEmail(), recipient.getPhone());
        }
    }
}
