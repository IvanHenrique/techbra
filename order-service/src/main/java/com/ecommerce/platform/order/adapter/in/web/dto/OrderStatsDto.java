package com.ecommerce.platform.order.adapter.in.web.dto;

import java.math.BigDecimal;

/**
 * DTO para estatísticas (futuro)
 */
record OrderStatsDto(
    long totalOrders,
    long pendingOrders,
    long confirmedOrders,
    long paidOrders,
    long shippedOrders,
    long deliveredOrders,
    long cancelledOrders,
    BigDecimal totalRevenue
) {}