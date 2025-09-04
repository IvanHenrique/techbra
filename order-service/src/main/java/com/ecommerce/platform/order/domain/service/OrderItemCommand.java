package com.ecommerce.platform.order.domain.service;

import com.ecommerce.platform.shared.domain.ValueObjects.*;

/**
 * OrderItemCommand - Command Object
 * Dados de um item do pedido
 */
public record OrderItemCommand(
    ProductId productId,
    Quantity quantity,
    Money unitPrice
) {
    public OrderItemCommand {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId não pode ser null");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity não pode ser null");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("UnitPrice não pode ser null");
        }
    }
}