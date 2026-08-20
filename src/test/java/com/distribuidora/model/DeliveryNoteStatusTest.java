package com.distribuidora.model;

import com.distribuidora.deliverynote.model.DeliveryNoteStatus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryNoteStatusTest {

    @Test
    void pendingCanGoToGeneratedOrCanceled() {
        assertThat(DeliveryNoteStatus.PENDING.canTransitionTo(DeliveryNoteStatus.GENERATED)).isTrue();
        assertThat(DeliveryNoteStatus.PENDING.canTransitionTo(DeliveryNoteStatus.CANCELED)).isTrue();
        assertThat(DeliveryNoteStatus.PENDING.canTransitionTo(DeliveryNoteStatus.DELIVERED)).isFalse();
        assertThat(DeliveryNoteStatus.PENDING.canTransitionTo(DeliveryNoteStatus.PENDING)).isFalse();
    }

    @Test
    void generatedCanGoToDeliveredOrCanceled() {
        assertThat(DeliveryNoteStatus.GENERATED.canTransitionTo(DeliveryNoteStatus.DELIVERED)).isTrue();
        assertThat(DeliveryNoteStatus.GENERATED.canTransitionTo(DeliveryNoteStatus.CANCELED)).isTrue();
        assertThat(DeliveryNoteStatus.GENERATED.canTransitionTo(DeliveryNoteStatus.PENDING)).isFalse();
        assertThat(DeliveryNoteStatus.GENERATED.canTransitionTo(DeliveryNoteStatus.GENERATED)).isFalse();
    }

    @Test
    void deliveredCannotTransitionToAnyState() {
        for (DeliveryNoteStatus target : DeliveryNoteStatus.values()) {
            assertThat(DeliveryNoteStatus.DELIVERED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void canceledCannotTransitionToAnyState() {
        for (DeliveryNoteStatus target : DeliveryNoteStatus.values()) {
            assertThat(DeliveryNoteStatus.CANCELED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void isTerminalMatchesDeliveredAndCanceled() {
        assertThat(DeliveryNoteStatus.DELIVERED.isTerminal()).isTrue();
        assertThat(DeliveryNoteStatus.CANCELED.isTerminal()).isTrue();
        assertThat(DeliveryNoteStatus.PENDING.isTerminal()).isFalse();
        assertThat(DeliveryNoteStatus.GENERATED.isTerminal()).isFalse();
    }
}
