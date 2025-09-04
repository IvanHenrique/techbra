package com.ecommerce.platform.order.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Item do pedido no request
 */
public record OrderItemRequestDto(
    @NotBlank(message = "Product ID é obrigatório")
    String productId,
    
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser pelo menos 1")
    @Max(value = 999, message = "Quantidade máxima é 999")
    Integer quantity,
    
    @NotNull(message = "Preço unitário é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "Preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
    BigDecimal unitPrice,
    
    String productName,
    String productDescription
) {}