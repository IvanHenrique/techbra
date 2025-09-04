package com.ecommerce.platform.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de criação de pedido
 * Disparado quando um novo pedido é criado no sistema
 */
public record OrderCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    Long aggregateVersion,
    Instant occurredOn,
    // Payload específico do evento
    UUID customerId,
    String customerEmail,
    java.math.BigDecimal totalAmount,
    String currency
) implements DomainEvent {
    
    @Override
    public String boundedContext() {
        return "orders";
    }
    
    /**
     * Factory method para criação simplificada
     */
    public static OrderCreatedEvent of(UUID orderId, UUID customerId, 
                                     String customerEmail, java.math.BigDecimal totalAmount) {
        return new OrderCreatedEvent(
            UUID.randomUUID(),
            orderId,
            1L,
            Instant.now(),
            customerId,
            customerEmail,
            totalAmount,
            "BRL"
        );
    }
}