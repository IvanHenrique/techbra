package com.ecommerce.platform.order.adapter.in.web.dto;

import java.util.List;

/**
 * DTO para listagem paginada (futuro)
 */
record OrderListResponseDto(
    List<OrderResponseDto> orders,
    int totalElements,
    int totalPages,
    int currentPage,
    int pageSize
) {}