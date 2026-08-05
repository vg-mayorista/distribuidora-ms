package com.distribuidora.model;

/**
 * Indicates the fulfillment flow of an {@link Order}:
 * <ul>
 *   <li>{@code WHOLESALE}: pedido que el cliente hace al catálogo general. No consume stock;
 *       el cliente elige un día de entrega según las ventanas semanales definidas.</li>
 *   <li>{@code STOCK}: pedido del cliente contra el excedente en depósito. Descuenta stock al
 *       confirmarse y permite envío express. La fecha de entrega queda en {@code null}.</li>
 * </ul>
 */
public enum OrderType {
    WHOLESALE,
    STOCK
}
