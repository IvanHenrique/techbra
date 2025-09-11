package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando um pedido é cancelado
 */
public record OrderCancelledEvent(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("occurredAt") LocalDateTime occurredAt,
    @JsonProperty("aggregateId") String aggregateId,
    @JsonProperty("orderId") String orderId,
    @JsonProperty("customerId") String customerId,
    @JsonProperty("orderTotal") BigDecimal orderTotal,
    @JsonProperty("currency") String currency,
    @JsonProperty("cancellationReason") String cancellationReason,
    @JsonProperty("cancelledAt") LocalDateTime cancelledAt,
    @JsonProperty("cancelledBy") String cancelledBy
) implements DomainEvent {

    @JsonCreator
    public OrderCancelledEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at cannot be null");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or blank");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
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
        return "ORDER_CANCELLED";
    }

    public static OrderCancelledEvent create(
            String orderId,
            String customerId,
            BigDecimal orderTotal,
            String currency,
            String cancellationReason,
            String cancelledBy) {
        
        return new OrderCancelledEvent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            orderId,
            orderId,
            customerId,
            orderTotal,
            currency,
            cancellationReason,
            LocalDateTime.now(),
            cancelledBy
        );
    }
}