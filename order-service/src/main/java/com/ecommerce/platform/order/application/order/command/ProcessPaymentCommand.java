package com.ecommerce.platform.order.application.order.command;

import com.ecommerce.platform.order.domain.enums.PaymentMethod;

import java.util.UUID;

public record ProcessPaymentCommand(
    UUID orderId,
    PaymentMethod paymentMethod,
    String paymentToken
) {
    public ProcessPaymentCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("OrderId não pode ser null");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("PaymentMethod não pode ser null");
        }
        if (paymentToken == null || paymentToken.trim().isEmpty()) {
            throw new IllegalArgumentException("PaymentToken não pode ser null ou vazio");
        }
    }
}