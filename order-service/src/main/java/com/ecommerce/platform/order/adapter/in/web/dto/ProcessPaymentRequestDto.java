package com.ecommerce.platform.order.adapter.in.web.dto;

import com.ecommerce.platform.order.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request para processar pagamento
 */
public record ProcessPaymentRequestDto(
    @NotNull(message = "Método de pagamento é obrigatório")
    PaymentMethod paymentMethod,
    
    @NotBlank(message = "Token de pagamento é obrigatório")
    String paymentToken
) {}