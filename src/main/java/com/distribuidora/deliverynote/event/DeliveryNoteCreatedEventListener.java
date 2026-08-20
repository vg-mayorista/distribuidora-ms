package com.distribuidora.deliverynote.event;

import com.distribuidora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryNoteCreatedEventListener {

    private final UserRepository userRepository;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeliveryNoteCreated(DeliveryNoteCreatedEvent event) {
        log.info("[deliverynote] DeliveryNoteCreatedEvent received post-commit for delivery note {} ({})",
                event.deliveryNoteId(), event.deliveryNoteNumber());

        String customerName = userRepository.findById(event.customerUserId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Cliente " + event.customerUserId());

        log.info("[deliverynote] Customer {} notified about delivery note {}",
                customerName, event.deliveryNoteNumber());
    }
}
