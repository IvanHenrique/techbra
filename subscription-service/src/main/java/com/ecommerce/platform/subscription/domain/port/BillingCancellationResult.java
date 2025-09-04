package com.ecommerce.platform.subscription.domain.port;

/**
 * BillingCancellationResult - Value Object
 */
public record BillingCancellationResult(boolean success, String message) {}

class BillingCancellationResults {
    public static BillingCancellationResult success() {
        return new BillingCancellationResult(true, "Cobrança cancelada com sucesso");
    }

    public static BillingCancellationResult failure(String message) {
        return new BillingCancellationResult(false, message);
    }
}