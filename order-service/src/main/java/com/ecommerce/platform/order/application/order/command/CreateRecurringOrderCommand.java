package com.ecommerce.platform.order.application.order.command;

import com.ecommerce.platform.shared.domain.ValueObjects;

import java.util.List;
import java.util.UUID;

/**
 * CreateRecurringOrderCommand - Command Object
 * Encapsula dados para criar pedido recorrente
 */
public record CreateRecurringOrderCommand(
    ValueObjects.CustomerId customerId,
    ValueObjects.CustomerEmail customerEmail,
    UUID subscriptionId,
    List<OrderItemCommand> items
) {
    public CreateRecurringOrderCommand {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId não pode ser null");
        }
        if (customerEmail == null) {
            throw new IllegalArgumentException("CustomerEmail não pode ser null");
        }
        if (subscriptionId == null) {
            throw new IllegalArgumentException("SubscriptionId não pode ser null");
        }
        if (items == null) {
            throw new IllegalArgumentException("Items não pode ser null");
        }
    }
}