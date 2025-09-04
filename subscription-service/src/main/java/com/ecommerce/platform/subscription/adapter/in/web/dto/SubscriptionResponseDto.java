package com.ecommerce.platform.subscription.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response com dados da assinatura
 */
public record SubscriptionResponseDto(
    UUID id,
    UUID customerId,
    String customerEmail,
    String planId,
    String status,
    String billingCycle,
    BigDecimal monthlyAmount,
    String currency,
    LocalDate startDate,
    LocalDate nextBillingDate,
    LocalDate endDate,
    Instant createdAt,
    Instant updatedAt,
    LocalDate gracePeriodEnd,
    Integer failedPaymentAttempts,
    String errorMessage
) {
    /**
     * Factory method para erro
     */
    public static SubscriptionResponseDto error(String errorMessage) {
        return new SubscriptionResponseDto(
            null, null, null, null, null, null, null, null, 
            null, null, null, null, null, null, null, errorMessage
        );
    }
    
    /**
     * Verifica se é resposta de erro
     */
    public boolean isError() {
        return errorMessage != null;
    }
}