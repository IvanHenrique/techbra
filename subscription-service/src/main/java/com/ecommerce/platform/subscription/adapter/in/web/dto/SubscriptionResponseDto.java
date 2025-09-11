package com.ecommerce.platform.subscription.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionResponseDto(
        UUID id,
        String customerId,
        String customerEmail,
        String planId,
        String status,
        String billingCycle,
        BigDecimal monthlyPrice,
        String currency,
        Integer trialPeriodDays,
        LocalDate nextBillingDate,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant cancelledAt,
        String errorMessage,
        boolean error
) implements Serializable {
    public static SubscriptionResponseDto error(String errorMessage) {
        return new SubscriptionResponseDto(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, errorMessage, true
        );
    }
}