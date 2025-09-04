package com.ecommerce.platform.subscription.adapter.in.web.dto;

/**
 * Response para processamento de cobrança
 */
public record ProcessBillingResponseDto(
    boolean success,
    String message,
    int successCount,
    int failureCount
) {}