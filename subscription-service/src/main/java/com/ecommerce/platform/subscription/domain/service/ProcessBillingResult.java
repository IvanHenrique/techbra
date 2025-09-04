package com.ecommerce.platform.subscription.domain.service;

/**
 * ProcessBillingResult - Result Object
 */
public record ProcessBillingResult(
    boolean success,
    String message,
    int successCount,
    int failureCount
) {
    public static ProcessBillingResult success(String message) {
        return new ProcessBillingResult(true, message, 1, 0);
    }
    
    public static ProcessBillingResult failure(String message) {
        return new ProcessBillingResult(false, message, 0, 1);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isFailure() {
        return !success;
    }
}