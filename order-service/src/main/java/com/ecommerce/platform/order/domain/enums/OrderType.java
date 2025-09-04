package com.ecommerce.platform.order.domain.enums;

/**
 * Enum: Tipo do Pedido
 * Distingue pedidos únicos de recorrentes
 */
public enum OrderType {
    ONE_TIME,              // Compra única
    SUBSCRIPTION_GENERATED // Gerado automaticamente por assinatura
}