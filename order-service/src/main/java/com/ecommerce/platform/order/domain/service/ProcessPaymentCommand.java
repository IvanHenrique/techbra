package com.ecommerce.platform.order.domain.service;

import com.ecommerce.platform.order.domain.port.PaymentMethod;

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