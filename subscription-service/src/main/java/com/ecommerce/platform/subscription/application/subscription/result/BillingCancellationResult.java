package com.ecommerce.platform.subscription.application.subscription.result;

/**
 * BillingCancellationResult - Value Object
 */
public record BillingCancellationResult(
        boolean success,
        String message
) {
    public static BillingCancellationResult success(String message) {
        return new BillingCancellationResult(true, message);
    }

    public static BillingCancellationResult failure(String message) {
        return new BillingCancellationResult(false, message);
    }
}