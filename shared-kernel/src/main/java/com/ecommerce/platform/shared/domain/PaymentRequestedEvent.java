package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando um pagamento é solicitado
 */
public record PaymentRequestedEvent(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("occurredAt") LocalDateTime occurredAt,
    @JsonProperty("aggregateId") String aggregateId,
    @JsonProperty("paymentId") String paymentId,
    @JsonProperty("orderId") String orderId,
    @JsonProperty("subscriptionId") String subscriptionId,
    @JsonProperty("customerId") String customerId,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") String currency,
    @JsonProperty("paymentMethod") String paymentMethod,
    @JsonProperty("requestedAt") LocalDateTime requestedAt
) implements DomainEvent {

    @JsonCreator
    public PaymentRequestedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at cannot be null");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or blank");
        }
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("Payment ID cannot be null or blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("Requested at cannot be null");
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
        return "PAYMENT_REQUESTED";
    }

    public static PaymentRequestedEvent forOrder(
            String paymentId,
            String orderId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethod) {
        
        return new PaymentRequestedEvent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            paymentId,
            paymentId,
            orderId,
            null,
            customerId,
            amount,
            currency,
            paymentMethod,
            LocalDateTime.now()
        );
    }

    public static PaymentRequestedEvent forSubscription(
            String paymentId,
            String subscriptionId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethod) {
        
        return new PaymentRequestedEvent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            paymentId,
            paymentId,
            null,
            subscriptionId,
            customerId,
            amount,
            currency,
            paymentMethod,
            LocalDateTime.now()
        );
    }

    public boolean isOrderPayment() {
        return orderId != null && subscriptionId == null;
    }

    public boolean isSubscriptionPayment() {
        return subscriptionId != null && orderId == null;
    }
}