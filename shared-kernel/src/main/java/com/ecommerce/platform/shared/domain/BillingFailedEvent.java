package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando uma cobrança falha
 */
public record BillingFailedEvent(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("occurredAt") LocalDateTime occurredAt,
    @JsonProperty("aggregateId") String aggregateId,
    @JsonProperty("billingId") String billingId,
    @JsonProperty("subscriptionId") String subscriptionId,
    @JsonProperty("customerId") String customerId,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") String currency,
    @JsonProperty("failureReason") String failureReason,
    @JsonProperty("failureCode") String failureCode,
    @JsonProperty("failedAt") LocalDateTime failedAt,
    @JsonProperty("retryAttempt") Integer retryAttempt,
    @JsonProperty("nextRetryDate") LocalDateTime nextRetryDate
) implements DomainEvent {

    @JsonCreator
    public BillingFailedEvent {
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
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("Failure reason cannot be null or blank");
        }
        if (failedAt == null) {
            throw new IllegalArgumentException("Failed at cannot be null");
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
        return "BILLING_FAILED";
    }

    public static BillingFailedEvent create(
            String billingId,
            String subscriptionId,
            String customerId,
            BigDecimal amount,
            String currency,
            String failureReason,
            String failureCode,
            Integer retryAttempt,
            LocalDateTime nextRetryDate) {
        
        return new BillingFailedEvent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            billingId,
            billingId,
            subscriptionId,
            customerId,
            amount,
            currency,
            failureReason,
            failureCode,
            LocalDateTime.now(),
            retryAttempt,
            nextRetryDate
        );
    }

    public boolean hasExhaustedRetries() {
        return retryAttempt != null && retryAttempt >= 3;
    }

    public boolean canRetry() {
        return nextRetryDate != null && !hasExhaustedRetries();
    }
}