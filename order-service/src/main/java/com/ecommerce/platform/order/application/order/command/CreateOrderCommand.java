package com.ecommerce.platform.order.application.order.command;

import com.ecommerce.platform.shared.domain.ValueObjects;

import java.util.List;

/**
 * CreateOrderCommand - Command Object
 * Encapsula dados necessários para criar um pedido
 */
public record CreateOrderCommand(
    ValueObjects.CustomerId customerId,
    ValueObjects.CustomerEmail customerEmail,
    List<OrderItemCommand> items
) {
    public CreateOrderCommand {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId não pode ser null");
        }
        if (customerEmail == null) {
            throw new IllegalArgumentException("CustomerEmail não pode ser null");
        }
        if (items == null) {
            throw new IllegalArgumentException("Items não pode ser null");
        }
    }
}