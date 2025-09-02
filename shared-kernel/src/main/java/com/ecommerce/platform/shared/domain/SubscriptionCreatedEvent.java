package com.ecommerce.platform.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de criação de assinatura
 * Disparado quando uma nova assinatura é criada
 */
record SubscriptionCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    Long aggregateVersion,
    Instant occurredOn,
    // Payload específico do evento
    UUID customerId,
    String planId,
    java.math.BigDecimal monthlyAmount,
    String billingCycle
) implements DomainEvent {
    
    @Override
    public String boundedContext() {
        return "subscriptions";
    }
    
    /**
     * Factory method para criação simplificada
     */
    public static SubscriptionCreatedEvent of(UUID subscriptionId, UUID customerId, 
                                            String planId, java.math.BigDecimal monthlyAmount) {
        return new SubscriptionCreatedEvent(
            UUID.randomUUID(),
            subscriptionId,
            1L,
            Instant.now(),
            customerId,
            planId,
            monthlyAmount,
            "MONTHLY"
        );
    }
}