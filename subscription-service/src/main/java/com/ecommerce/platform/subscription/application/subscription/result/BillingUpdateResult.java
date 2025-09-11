package com.ecommerce.platform.subscription.application.subscription.result;

/**
 * BillingUpdateResult - Value Object
 */
public record BillingUpdateResult(
        boolean success,
        String message
) {
    public static BillingUpdateResult success(String message) {
        return new BillingUpdateResult(true, message);
    }

    public static BillingUpdateResult failure(String message) {
        return new BillingUpdateResult(false, message);
    }
}
