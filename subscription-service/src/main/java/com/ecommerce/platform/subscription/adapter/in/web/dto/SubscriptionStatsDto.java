package com.ecommerce.platform.subscription.adapter.in.web.dto;

import java.math.BigDecimal;

/**
 * DTO para estatísticas de assinaturas (futuro)
 */
public record SubscriptionStatsDto(
    long totalSubscriptions,
    long activeSubscriptions,
    long pendingSubscriptions,
    long pausedSubscriptions,
    long pastDueSubscriptions,
    long cancelledSubscriptions,
    BigDecimal monthlyRecurringRevenue,
    BigDecimal averageRevenuePerUser
) {}