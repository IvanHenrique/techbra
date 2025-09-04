package com.ecommerce.platform.order.domain.port;

import com.ecommerce.platform.shared.domain.ValueObjects;

import java.util.UUID;

/**
 * PaymentRequest - Value Object
 * Encapsula dados necessários para processamento de pagamento
 */
public record PaymentRequest(
    UUID orderId,
    ValueObjects.CustomerId customerId,
    ValueObjects.CustomerEmail customerEmail,
    ValueObjects.Money amount,
    PaymentMethod paymentMethod,
    String paymentToken // Token seguro do método de pagamento
) {
    public PaymentRequest {
        if (orderId == null) {
            throw new IllegalArgumentException("OrderId não pode ser null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId não pode ser null");
        }
        if (customerEmail == null) {
            throw new IllegalArgumentException("CustomerEmail não pode ser null");
        }
        if (amount == null || amount.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount deve ser positivo");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("PaymentMethod não pode ser null");
        }
        if (paymentToken == null || paymentToken.trim().isEmpty()) {
            throw new IllegalArgumentException("PaymentToken não pode ser null ou vazio");
        }
    }
}