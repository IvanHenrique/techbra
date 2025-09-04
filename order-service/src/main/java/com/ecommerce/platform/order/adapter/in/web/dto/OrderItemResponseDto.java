package com.ecommerce.platform.order.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item do pedido no response
 */
public record OrderItemResponseDto(
    UUID id,
    UUID productId,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    String productName,
    String productDescription
) {}