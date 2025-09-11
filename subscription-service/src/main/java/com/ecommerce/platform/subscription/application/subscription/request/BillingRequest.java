package com.ecommerce.platform.subscription.application.subscription.request;

import com.ecommerce.platform.shared.domain.ValueObjects.CustomerId;
import com.ecommerce.platform.shared.domain.ValueObjects.Money;

import java.util.UUID;

/**
 * BillingRequest - Value Object
 * Dados para processar cobrança imediata
 */
public record BillingRequest(
    UUID subscriptionId,
    CustomerId customerId,
    Money amount,
    String paymentMethodToken,
    boolean isRetry
) {
    public BillingRequest {
        if (subscriptionId == null) {
            throw new IllegalArgumentException("SubscriptionId não pode ser null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId não pode ser null");
        }
        if (amount == null || amount.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount deve ser positivo");
        }
        if (paymentMethodToken == null || paymentMethodToken.trim().isEmpty()) {
            throw new IllegalArgumentException("PaymentMethodToken não pode ser null ou vazio");
        }
    }
}