package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando uma assinatura é cancelada
 */
public record SubscriptionCancelledEvent(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("occurredAt") LocalDateTime occurredAt,
    @JsonProperty("aggregateId") String aggregateId,
    @JsonProperty("subscriptionId") String subscriptionId,
    @JsonProperty("customerId") String customerId,
    @JsonProperty("planId") String planId,
    @JsonProperty("monthlyPrice") BigDecimal monthlyPrice,
    @JsonProperty("currency") String currency,
    @JsonProperty("cancellationReason") String cancellationReason,
    @JsonProperty("cancelledAt") LocalDateTime cancelledAt,
    @JsonProperty("cancelledBy") String cancelledBy,
    @JsonProperty("effectiveDate") LocalDateTime effectiveDate
) implements DomainEvent {

    @JsonCreator
    public SubscriptionCancelledEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at cannot be null");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or blank");
        }
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("Subscription ID cannot be null or blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (cancellationReason == null || cancellationReason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or blank");
        }
        if (cancelledAt == null) {
            throw new IllegalArgumentException("Cancelled at cannot be null");
        }
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    @Override
    public String getEventType() {
        return "SUBSCRIPTION_CANCELLED";
    }

    public static SubscriptionCancelledEvent create(
            String subscriptionId,
            String customerId,
            String planId,
            BigDecimal monthlyPrice,
            String currency,
            String cancellationReason,
            String cancelledBy,
            LocalDateTime effectiveDate) {
        
        return new SubscriptionCancelledEvent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            subscriptionId,
            subscriptionId,
            customerId,
            planId,
            monthlyPrice,
            currency,
            cancellationReason,
            LocalDateTime.now(),
            cancelledBy,
            effectiveDate
        );
    }
}