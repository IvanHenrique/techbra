package com.ecommerce.platform.subscription.domain.port;

/**
 * BillingUpdateResult - Value Object
 */
public record BillingUpdateResult(
    boolean success,
    String message
) {}

class BillingUpdateResults {
    public static BillingUpdateResult success() {
        return new BillingUpdateResult(true, "Cobrança atualizada com sucesso");
    }

    public static BillingUpdateResult failure(String message) {
        return new BillingUpdateResult(false, message);
    }
}
