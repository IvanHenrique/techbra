package com.ecommerce.platform.subscription.adapter.in.web.dto;

import com.ecommerce.platform.subscription.domain.model.BillingCycle;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTOs para Subscription API
 * Data Transfer Objects que definem os contratos da API REST.
 * Separados do domínio para permitir evolução independente
 * da API sem impactar o core business logic de assinaturas.
 */

/**
 * Request para criar assinatura
 */
public record CreateSubscriptionRequestDto(
    @NotBlank(message = "Customer ID é obrigatório")
    String customerId,
    
    @NotBlank(message = "Email do cliente é obrigatório")
    @Email(message = "Email deve ter formato válido")
    String customerEmail,
    
    @NotBlank(message = "Plan ID é obrigatório")
    String planId,
    
    @NotNull(message = "Ciclo de cobrança é obrigatório")
    BillingCycle billingCycle,
    
    @NotNull(message = "Valor mensal é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor mensal deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "Valor deve ter no máximo 8 dígitos inteiros e 2 decimais")
    BigDecimal monthlyAmount,
    
    @NotBlank(message = "Token do método de pagamento é obrigatório")
    String paymentMethodToken
) {}