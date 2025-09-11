package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando uma cobrança é processada com sucesso
 */
public record BillingProcessedEvent(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("occurredAt") LocalDateTime occurredAt,
    @JsonProperty("aggregateId") String aggregateId,
    @JsonProperty("billingId") String billingId,
    @JsonProperty("subscriptionId") String subscriptionId,
    @JsonProperty("customerId") String customerId,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") String currency,
    @JsonProperty("processedAt") LocalDateTime processedAt,
    @JsonProperty("paymentId") String paymentId,
    @JsonProperty("nextBillingDate") LocalDateTime nextBillingDate
) implements DomainEvent {

    @JsonCreator
    public BillingProcessedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at cannot be null");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or blank");
        }
        if (billingId == null || billingId.isBlank()) {
            throw new IllegalArgumentException("Billing ID cannot be null or blank");
        }
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("Subscription ID cannot be null or blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (processedAt == null) {
            throw new IllegalArgumentException("Processed at cannot be null");
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
        return "BILLING_PROCESSED";
    }

    public static BillingProcessedEvent create(
            String billingId,
            String subscriptionId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentId,
            LocalDateTime nextBillingDate) {
        
        return new BillingProcessedEvent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            billingId,
            billingId,
            subscriptionId,
            customerId,
            amount,
            currency,
            LocalDateTime.now(),
            paymentId,
            nextBillingDate
        );
    }
}