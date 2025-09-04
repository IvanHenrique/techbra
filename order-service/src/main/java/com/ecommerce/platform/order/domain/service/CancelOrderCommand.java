package com.ecommerce.platform.order.domain.service;

import java.util.UUID;

public record CancelOrderCommand(
    UUID orderId,
    String reason
) {
    public CancelOrderCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("OrderId não pode ser null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Motivo do cancelamento é obrigatório");
        }
    }
}