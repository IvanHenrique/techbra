package com.ecommerce.platform.subscription.adapter.in.web.dto;

import com.ecommerce.platform.subscription.domain.enums.BillingCycle;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateSubscriptionRequestDto(
        @NotBlank(message = "ID do cliente é obrigatório")
        String customerId,

        @NotBlank(message = "Email do cliente é obrigatório")
        @Email(message = "Email deve ter formato válido")
        String customerEmail,

        @NotBlank(message = "ID do plano é obrigatório")
        String planId,

        @NotNull(message = "Ciclo de cobrança é obrigatório")
        BillingCycle billingCycle,

        @NotNull(message = "Preço mensal é obrigatório")
        @Positive(message = "Preço mensal deve ser positivo")
        BigDecimal monthlyPrice,

        @Min(value = 0, message = "Período de trial não pode ser negativo")
        @Max(value = 365, message = "Período de trial não pode exceder 365 dias")
        Integer trialPeriodDays,

        @NotBlank(message = "ID do método de pagamento é obrigatório")
        String paymentMethodId
) {}