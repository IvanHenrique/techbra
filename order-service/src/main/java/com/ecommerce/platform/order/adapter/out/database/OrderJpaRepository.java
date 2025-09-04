package com.ecommerce.platform.order.adapter.out.database;

import com.ecommerce.platform.order.domain.model.Order;
import com.ecommerce.platform.order.domain.port.OrderRepository;
import com.ecommerce.platform.shared.domain.ValueObjects.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OrderJpaRepository - Adapter de Saída (Database)
 * Implementa o port OrderRepository usando Spring Data JPA.
 * Atua como adapter na arquitetura hexagonal, convertendo
 * operações de domínio em queries de banco de dados.
 * Responsabilidades:
 * - Implementar todas as operações definidas no port
 * - Converter entre entidades JPA e objetos de domínio
 * - Otimizar queries para performance
 * - Manter consistência transacional
 * Padrões aplicados:
 * - Repository Pattern: Abstração da persistência
 * - Adapter Pattern: Adapta JPA para domínio
 * - Specification Pattern: Queries complexas
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<Order, UUID>, OrderRepository {
    
    /**
     * Busca pedidos por ID do cliente
     * Ordenados por data de criação (mais recentes primeiro)
     */
    @Query("SELECT o FROM Order o WHERE o.customerId.value = :customerId ORDER BY o.createdAt DESC")
    List<Order> findByCustomerId(@Param("customerId") CustomerId customerId);
    
    /**
     * Busca pedidos por status
     * 
     * @param status Status como string (convertido automaticamente)
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status")
    List<Order> findByStatus(@Param("status") String status);
    
    /**
     * Busca pedidos recorrentes por ID da assinatura
     */
    @Query("SELECT o FROM Order o WHERE o.subscriptionId = :subscriptionId ORDER BY o.createdAt DESC")
    List<Order> findBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);
    
    /**
     * Verifica se cliente possui pedidos ativos
     * 
     * Considera ativos: PENDING, CONFIRMED, PAID, SHIPPED
     */
    @Query("""
        SELECT COUNT(o) > 0 FROM Order o 
        WHERE o.customerId.value = :customerId 
        AND o.status IN ('PENDING', 'CONFIRMED', 'PAID', 'SHIPPED')
        """)
    boolean hasActiveOrders(@Param("customerId") CustomerId customerId);
    
    /**
     * Conta total de pedidos do cliente
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customerId.value = :customerId")
    long countByCustomerId(@Param("customerId") CustomerId customerId);
    
    /**
     * Busca pedidos pendentes mais antigos que X minutos
     * 
     * Útil para identificar pedidos abandonados
     */
    @Query("""
        SELECT o FROM Order o 
        WHERE o.status = 'PENDING' 
        AND o.createdAt < :cutoffTime
        ORDER BY o.createdAt ASC
        """)
    List<Order> findPendingOrdersOlderThan(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Implementação customizada do método do port
     */
    default List<Order> findPendingOrdersOlderThan(int minutesAgo) {
        Instant cutoffTime = Instant.now().minusSeconds(minutesAgo * 60L);
        return findPendingOrdersOlderThan(cutoffTime);
    }
    
    /**
     * Queries adicionais para relatórios e analytics
     */
    
    /**
     * Busca pedidos por período
     */
    @Query("""
        SELECT o FROM Order o 
        WHERE o.createdAt BETWEEN :startDate AND :endDate
        ORDER BY o.createdAt DESC
        """)
    List<Order> findOrdersBetweenDates(@Param("startDate") Instant startDate, 
                                      @Param("endDate") Instant endDate);
    
    /**
     * Estatísticas de pedidos por status
     */
    @Query("""
        SELECT o.status as status, COUNT(o) as count, SUM(o.totalAmount.amount) as total
        FROM Order o 
        GROUP BY o.status
        """)
    List<OrderStatusStats> getOrderStatsByStatus();
    
    /**
     * Pedidos por cliente no período
     */
    @Query("""
        SELECT o FROM Order o 
        WHERE o.customerId.value = :customerId 
        AND o.createdAt BETWEEN :startDate AND :endDate
        ORDER BY o.createdAt DESC
        """)
    List<Order> findCustomerOrdersInPeriod(@Param("customerId") CustomerId customerId,
                                          @Param("startDate") Instant startDate,
                                          @Param("endDate") Instant endDate);
    
    /**
     * Top produtos mais vendidos
     */
    @Query("""
        SELECT oi.productId.value as productId, 
               SUM(oi.quantity.value) as totalQuantity,
               SUM(oi.quantity.value * oi.unitPrice.amount) as totalRevenue
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED')
        GROUP BY oi.productId.value
        ORDER BY totalQuantity DESC
        """)
    List<ProductSalesStats> getTopSellingProducts();
    
    /**
     * Pedidos que precisam ser processados automaticamente
     * Ex: pedidos confirmados há mais de X tempo sem pagamento
     */
    @Query("""
        SELECT o FROM Order o 
        WHERE o.status = 'CONFIRMED' 
        AND o.updatedAt < :cutoffTime
        ORDER BY o.updatedAt ASC
        """)
    List<Order> findStaleConfirmedOrders(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Busca com fetch join para evitar N+1 queries
     */
    @Query("""
        SELECT DISTINCT o FROM Order o 
        LEFT JOIN FETCH o.items 
        WHERE o.id = :orderId
        """)
    Optional<Order> findByIdWithItems(@Param("orderId") UUID orderId);
    
    /**
     * Busca pedidos do cliente com itens (otimizada)
     */
    @Query("""
        SELECT DISTINCT o FROM Order o 
        LEFT JOIN FETCH o.items 
        WHERE o.customerId.value = :customerId
        ORDER BY o.createdAt DESC
        """)
    List<Order> findByCustomerIdWithItems(@Param("customerId") CustomerId customerId);
}