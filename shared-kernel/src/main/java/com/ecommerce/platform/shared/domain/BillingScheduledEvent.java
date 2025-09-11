package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando uma cobrança é agendada
 */
public record BillingScheduledEvent(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("occurredAt") LocalDateTime occurredAt,
    @JsonProperty("aggregateId") String aggregateId,
    @JsonProperty("billingId") String billingId,
    @JsonProperty("subscriptionId") String subscriptionId,
    @JsonProperty("customerId") String customerId,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") String currency,
    @JsonProperty("scheduledDate") LocalDateTime scheduledDate,
    @JsonProperty("billingCycle") String billingCycle,
    @JsonProperty("paymentMethodId") String paymentMethodId
) implements DomainEvent {

    @JsonCreator
    public BillingScheduledEvent {
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
        if (scheduledDate == null) {
            throw new IllegalArgumentException("Scheduled date cannot be null");
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
        return "BILLING_SCHEDULED";
    }

    public static BillingScheduledEvent create(
            String billingId,
            String subscriptionId,
            String customerId,
            BigDecimal amount,
            String currency,
            LocalDateTime scheduledDate,
            String billingCycle,
            String paymentMethodId) {
        
        return new BillingScheduledEvent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            billingId,
            billingId,
            subscriptionId,
            customerId,
            amount,
            currency,
            scheduledDate,
            billingCycle,
            paymentMethodId
        );
    }

    public boolean isMonthlyBilling() {
        return "MONTHLY".equals(billingCycle);
    }

    public boolean isYearlyBilling() {
        return "YEARLY".equals(billingCycle);
    }
}