package com.distribuidora.notification.event;

import com.distribuidora.notification.dispatcher.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final NotificationDispatcher notificationDispatcher;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderNotification(OrderNotificationEvent event) {
        log.info("[notification] OrderNotificationEvent received post-commit for order {} ({})",
                event.getOrderId(), event.getOrderType());

        notificationDispatcher.dispatchOrderCreatedNotification(event);
    }
}
