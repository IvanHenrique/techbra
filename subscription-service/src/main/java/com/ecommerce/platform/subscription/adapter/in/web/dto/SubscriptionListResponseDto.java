package com.ecommerce.platform.subscription.adapter.in.web.dto;

/**
 * DTO para listagem paginada (futuro)
 */
public record SubscriptionListResponseDto(
    java.util.List<SubscriptionResponseDto> subscriptions,
    int totalElements,
    int totalPages,
    int currentPage,
    int pageSize
) {}