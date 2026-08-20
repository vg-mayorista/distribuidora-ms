package com.distribuidora.deliverynote.event;

import java.util.UUID;

public record DeliveryNoteCreatedEvent(
        UUID deliveryNoteId,
        String deliveryNoteNumber,
        UUID customerUserId
) {}
