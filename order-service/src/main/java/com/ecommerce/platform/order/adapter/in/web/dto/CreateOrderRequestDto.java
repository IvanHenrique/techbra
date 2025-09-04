package com.ecommerce.platform.order.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.util.List;

/**
 * DTOs para Order API
 * Data Transfer Objects que definem os contratos da API REST.
 * Separados do domínio para permitir evolução independente
 * da API sem impactar o core business logic.
 */

/**
 * Request para criar pedido
 */
public record CreateOrderRequestDto(
    @NotBlank(message = "Customer ID é obrigatório")
    String customerId,
    
    @NotBlank(message = "Email do cliente é obrigatório")
    @Email(message = "Email deve ter formato válido")
    String customerEmail,
    
    @NotNull(message = "Items são obrigatórios")
    @NotEmpty(message = "Pedido deve ter pelo menos um item")
    @Size(max = 50, message = "Pedido não pode ter mais de 50 itens")
    List<OrderItemRequestDto> items
) {}