package com.ecommerce.platform.subscription.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO para status da assinatura
 */
public record SubscriptionStatusDto(
    UUID subscriptionId,
    String status,
    LocalDate nextBillingDate,
    Boolean inGracePeriod,
    Integer failedPaymentAttempts
) {}