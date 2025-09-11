package com.ecommerce.platform.order.adapter.in.web.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response com dados do pedido
 */
public record OrderResponseDto(
    UUID id,
    UUID customerId,
    String customerEmail,
    String status,
    String type,
    BigDecimal totalAmount,
    String currency,
    Instant createdAt,
    Instant updatedAt,
    UUID subscriptionId,
    List<OrderItemResponseDto> items,
    String errorMessage
) implements Serializable {
    /**
     * Factory method para erro
     */
    public static OrderResponseDto error(String errorMessage) {
        return new OrderResponseDto(
            null, null, null, null, null, null, null, 
            null, null, null, null, errorMessage
        );
    }
    
    /**
     * Verifica se é resposta de erro
     */
    public boolean isError() {
        return errorMessage != null;
    }
}