package com.ecommerce.platform.subscription.application.subscription.command;

import com.ecommerce.platform.subscription.domain.enums.BillingCycle;

/**
 * Command para criação de assinatura
 */
public record CreateSubscriptionCommand(
        String customerId,
        String customerEmail,
        String planId,
        BillingCycle billingCycle,
        java.math.BigDecimal monthlyPrice,
        Integer trialPeriodDays,
        String paymentMethodId
) {
    public CreateSubscriptionCommand {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("CustomerId é obrigatório");
        }
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("CustomerEmail é obrigatório");
        }
        if (planId == null || planId.trim().isEmpty()) {
            throw new IllegalArgumentException("PlanId é obrigatório");
        }
        if (billingCycle == null) {
            throw new IllegalArgumentException("BillingCycle é obrigatório");
        }
        if (monthlyPrice == null || monthlyPrice.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("MonthlyPrice deve ser maior que zero");
        }
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            throw new IllegalArgumentException("PaymentMethodId é obrigatório");
        }
    }
}