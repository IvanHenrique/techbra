package com.ecommerce.platform.subscription.application.subscription.result;

import com.ecommerce.platform.subscription.domain.model.Subscription;

/**
 * Resultado da criação de assinatura
 */
public record CreateSubscriptionResult(
        boolean success,
        Subscription subscription,
        String errorMessage
) {
    public static CreateSubscriptionResult success(Subscription subscription) {
        return new CreateSubscriptionResult(true, subscription, null);
    }

    public static CreateSubscriptionResult failure(String errorMessage) {
        return new CreateSubscriptionResult(false, null, errorMessage);
    }
}