package com.ecommerce.platform.order.application.order.command;

import java.util.UUID;

// Command Objects
public record ConfirmOrderCommand(UUID orderId) {
    public ConfirmOrderCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("OrderId não pode ser null");
        }
    }
}