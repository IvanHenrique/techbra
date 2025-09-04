package com.ecommerce.platform.order.adapter.out.database;

import java.util.UUID;

/**
 * Projection para estatísticas de vendas por produto
 */
interface ProductSalesStats {
    UUID getProductId();
    Long getTotalQuantity();
    java.math.BigDecimal getTotalRevenue();
}