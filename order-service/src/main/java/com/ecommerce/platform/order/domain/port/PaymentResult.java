package com.ecommerce.platform.order.domain.port;

/**
 * PaymentResult - Value Object
 * Encapsula resultado do processamento de pagamento
 */
public record PaymentResult(
    boolean success,
    String paymentId,
    PaymentStatus status,
    String message,
    String errorCode
) {
    public static PaymentResult success(String paymentId, PaymentStatus status) {
        return new PaymentResult(true, paymentId, status, "Pagamento processado com sucesso", null);
    }
    
    public static PaymentResult failure(String errorCode, String message) {
        return new PaymentResult(false, null, PaymentStatus.FAILED, message, errorCode);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isFailure() {
        return !success;
    }
}