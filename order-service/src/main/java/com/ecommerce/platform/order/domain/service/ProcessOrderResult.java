package com.ecommerce.platform.order.domain.service;

import com.ecommerce.platform.order.domain.model.Order;

// Result Object
public record ProcessOrderResult(
    boolean success,
    Order order,
    String errorMessage
) {
    public static ProcessOrderResult success(Order order) {
        return new ProcessOrderResult(true, order, null);
    }
    
    public static ProcessOrderResult failure(String errorMessage) {
        return new ProcessOrderResult(false, null, errorMessage);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isFailure() {
        return !success;
    }
}