package com.ecommerce.platform.subscription.domain.service;

import com.ecommerce.platform.subscription.domain.model.Subscription;

/**
 * CreateSubscriptionResult - Result Object
 * Resultado da operação de criação
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
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isFailure() {
        return !success;
    }
}