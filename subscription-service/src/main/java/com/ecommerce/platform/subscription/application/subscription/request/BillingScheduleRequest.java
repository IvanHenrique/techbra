package com.ecommerce.platform.subscription.application.subscription.request;

import com.ecommerce.platform.shared.domain.ValueObjects.*;
import com.ecommerce.platform.subscription.domain.enums.BillingCycle;

import java.time.LocalDate;
import java.util.UUID;

/**
 * BillingScheduleRequest - Value Object
 * Dados para agendar cobrança recorrente
 */
public record BillingScheduleRequest(
    UUID subscriptionId,
    CustomerId customerId,
    CustomerEmail customerEmail,
    Money amount,
    BillingCycle cycle,
    LocalDate firstBillingDate,
    String paymentMethodToken
) {
    public BillingScheduleRequest {
        if (subscriptionId == null) {
            throw new IllegalArgumentException("SubscriptionId não pode ser null");
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
        if (cycle == null) {
            throw new IllegalArgumentException("BillingCycle não pode ser null");
        }
        if (firstBillingDate == null) {
            throw new IllegalArgumentException("FirstBillingDate não pode ser null");
        }
        if (paymentMethodToken == null || paymentMethodToken.trim().isEmpty()) {
            throw new IllegalArgumentException("PaymentMethodToken não pode ser null ou vazio");
        }
    }
}