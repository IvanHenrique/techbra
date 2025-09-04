package com.ecommerce.platform.order.adapter.out.messaging;

/**
 * Exceção customizada para falhas de publicação
 */
class EventPublishException extends RuntimeException {
    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}