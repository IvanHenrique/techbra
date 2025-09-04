package com.ecommerce.platform.subscription.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para mudança de plano (futuro)
 */
public record ChangePlanRequestDto(
    @NotBlank(message = "Novo Plan ID é obrigatório")
    String newPlanId,
    
    @NotNull(message = "Novo valor mensal é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    BigDecimal newMonthlyAmount,
    
    boolean applyImmediately
) {}