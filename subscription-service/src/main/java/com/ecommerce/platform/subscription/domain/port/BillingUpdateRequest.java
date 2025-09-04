package com.ecommerce.platform.subscription.domain.port;

import com.ecommerce.platform.shared.domain.ValueObjects.Money;
import com.ecommerce.platform.subscription.domain.model.BillingCycle;

import java.util.UUID;

/**
 * BillingUpdateRequest - Value Object
 * Dados para atualizar cobrança
 */
record BillingUpdateRequest(
    UUID subscriptionId,
    Money newAmount,
    BillingCycle newCycle,
    String newPaymentMethodToken
) {
    public BillingUpdateRequest {
        if (subscriptionId == null) {
            throw new IllegalArgumentException("SubscriptionId não pode ser null");
        }
    }
}