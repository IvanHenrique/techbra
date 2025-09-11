package com.ecommerce.platform.order.application.order.result;

import com.ecommerce.platform.order.domain.model.Order;

/**
 * CreateOrderResult - Result Object
 * Resultado da operação de criação
 */
public record CreateOrderResult(
    boolean success,
    Order order,
    String errorMessage
) {
    public static CreateOrderResult success(Order order) {
        return new CreateOrderResult(true, order, null);
    }
    
    public static CreateOrderResult failure(String errorMessage) {
        return new CreateOrderResult(false, null, errorMessage);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isFailure() {
        return !success;
    }
}