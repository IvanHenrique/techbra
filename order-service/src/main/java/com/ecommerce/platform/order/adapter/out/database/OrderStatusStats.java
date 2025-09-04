package com.ecommerce.platform.order.adapter.out.database;

/**
 * Projection para estatísticas de status
 */
interface OrderStatusStats {
    String getStatus();
    Long getCount();
    java.math.BigDecimal getTotal();
}