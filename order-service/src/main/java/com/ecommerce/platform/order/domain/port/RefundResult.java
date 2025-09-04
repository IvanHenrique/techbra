package com.ecommerce.platform.order.domain.port;

import com.ecommerce.platform.shared.domain.ValueObjects;

/**
 * RefundResult - Value Object
 * Encapsula resultado do reembolso
 */
record RefundResult(
    boolean success,
    String refundId,
    ValueObjects.Money refundedAmount,
    String message
) {
    public static RefundResult success(String refundId, ValueObjects.Money amount) {
        return new RefundResult(true, refundId, amount, "Reembolso processado com sucesso");
    }
    
    public static RefundResult failure(String message) {
        return new RefundResult(false, null, ValueObjects.Money.zero(), message);
    }
}