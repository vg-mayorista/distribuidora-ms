package com.distribuidora.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void pendingCanGoToArmadoOrCancelado() {
        assertThat(OrderStatus.PENDIENTE.canTransitionTo(OrderStatus.ARMADO)).isTrue();
        assertThat(OrderStatus.PENDIENTE.canTransitionTo(OrderStatus.CANCELADO)).isTrue();
        assertThat(OrderStatus.PENDIENTE.canTransitionTo(OrderStatus.ENVIADO)).isFalse();
        assertThat(OrderStatus.PENDIENTE.canTransitionTo(OrderStatus.ENTREGADO)).isFalse();
    }

    @Test
    void armadoCanGoToEnviadoOrCancelado() {
        assertThat(OrderStatus.ARMADO.canTransitionTo(OrderStatus.ENVIADO)).isTrue();
        assertThat(OrderStatus.ARMADO.canTransitionTo(OrderStatus.CANCELADO)).isTrue();
        assertThat(OrderStatus.ARMADO.canTransitionTo(OrderStatus.PENDIENTE)).isFalse();
        assertThat(OrderStatus.ARMADO.canTransitionTo(OrderStatus.ENTREGADO)).isFalse();
    }

    @Test
    void enviadoOnlyGoesToEntregado() {
        assertThat(OrderStatus.ENVIADO.canTransitionTo(OrderStatus.ENTREGADO)).isTrue();
        assertThat(OrderStatus.ENVIADO.canTransitionTo(OrderStatus.CANCELADO)).isFalse();
        assertThat(OrderStatus.ENVIADO.canTransitionTo(OrderStatus.ARMADO)).isFalse();
        assertThat(OrderStatus.ENVIADO.canTransitionTo(OrderStatus.PENDIENTE)).isFalse();
    }

    @Test
    void terminalStatesCannotTransition() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.ENTREGADO.canTransitionTo(target)).isFalse();
            assertThat(OrderStatus.CANCELADO.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void isTerminalMatches() {
        assertThat(OrderStatus.ENTREGADO.isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELADO.isTerminal()).isTrue();
        assertThat(OrderStatus.PENDIENTE.isTerminal()).isFalse();
        assertThat(OrderStatus.ARMADO.isTerminal()).isFalse();
        assertThat(OrderStatus.ENVIADO.isTerminal()).isFalse();
    }
}
