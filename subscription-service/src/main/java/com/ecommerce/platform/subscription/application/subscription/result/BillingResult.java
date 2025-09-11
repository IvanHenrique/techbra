package com.ecommerce.platform.subscription.application.subscription.result;

import com.ecommerce.platform.shared.domain.ValueObjects.Money;

/**
 * BillingResult - Value Object
 * Resultado do processamento de cobrança
 */
public record BillingResult(
    boolean success,
    String transactionId,
    Money chargedAmount,
    String message,
    String errorCode
) {
    public static BillingResult success(String transactionId, Money chargedAmount) {
        return new BillingResult(true, transactionId, chargedAmount, "Cobrança processada com sucesso", null);
    }
    
    public static BillingResult failure(String errorCode, String message) {
        return new BillingResult(false, null, Money.zero(), message, errorCode);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isFailure() {
        return !success;
    }
}