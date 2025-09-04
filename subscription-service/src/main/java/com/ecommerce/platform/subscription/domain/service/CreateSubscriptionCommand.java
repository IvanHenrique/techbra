package com.ecommerce.platform.subscription.domain.service;

import com.ecommerce.platform.shared.domain.ValueObjects.CustomerEmail;
import com.ecommerce.platform.shared.domain.ValueObjects.CustomerId;
import com.ecommerce.platform.shared.domain.ValueObjects.Money;
import com.ecommerce.platform.subscription.domain.model.BillingCycle;

/**
 * CreateSubscriptionCommand - Command Object
 * Encapsula dados necessários para criar uma assinatura
 */
public record CreateSubscriptionCommand(
    CustomerId customerId,
    CustomerEmail customerEmail,
    String planId,
    BillingCycle billingCycle,
    Money monthlyAmount,
    String paymentMethodToken
) {
    public CreateSubscriptionCommand {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId não pode ser null");
        }
        if (customerEmail == null) {
            throw new IllegalArgumentException("CustomerEmail não pode ser null");
        }
        if (planId == null || planId.trim().isEmpty()) {
            throw new IllegalArgumentException("PlanId não pode ser null ou vazio");
        }
        if (billingCycle == null) {
            throw new IllegalArgumentException("BillingCycle não pode ser null");
        }
        if (monthlyAmount == null) {
            throw new IllegalArgumentException("MonthlyAmount não pode ser null");
        }
        if (paymentMethodToken == null || paymentMethodToken.trim().isEmpty()) {
            throw new IllegalArgumentException("PaymentMethodToken não pode ser null ou vazio");
        }
    }
}