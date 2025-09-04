package com.ecommerce.platform.order.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO simplificado apenas com status
 */
public record OrderStatusDto(
    UUID orderId,
    String status,
    Instant updatedAt
) {}