package com.distribuidora.model;

/**
 * Restricts a {@link DeliveryMethod} to one or both order flows.
 * <ul>
 *   <li>{@code WHOLESALE}: solo para pedidos del catálogo general (sin stock).</li>
 *   <li>{@code STOCK}: solo para pedidos del excedente en depósito (compra inmediata).</li>
 *   <li>{@code BOTH}: disponible en ambos flujos.</li>
 * </ul>
 */
public enum DeliveryMethodScope {
    WHOLESALE,
    STOCK,
    BOTH
}
