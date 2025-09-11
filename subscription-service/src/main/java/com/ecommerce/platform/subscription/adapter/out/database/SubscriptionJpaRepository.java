package com.ecommerce.platform.subscription.adapter.out.database;

import com.ecommerce.platform.shared.domain.ValueObjects.CustomerId;
import com.ecommerce.platform.subscription.domain.model.Subscription;
import com.ecommerce.platform.subscription.domain.port.SubscriptionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SubscriptionJpaRepository - Adapter de Saída (Database)
 * Implementa o port SubscriptionRepository usando Spring Data JPA.
 */
@Repository
public interface SubscriptionJpaRepository extends JpaRepository<Subscription, UUID>, SubscriptionRepository {

    /**
     * Busca assinaturas por ID do cliente
     */
    @Query("SELECT s FROM Subscription s WHERE s.customerId = :customerId ORDER BY s.createdAt DESC")
    List<Subscription> findByCustomerId(@Param("customerId") CustomerId customerId);

    /**
     * Busca assinaturas por status
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = :status")
    List<Subscription> findByStatus(@Param("status") String status);

    /**
     * Busca assinaturas ativas que precisam ser cobradas
     */
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'ACTIVE' 
        AND s.nextBillingDate <= :billingDate
        ORDER BY s.nextBillingDate ASC
        """)
    List<Subscription> findActiveSubscriptionsDueForBilling(@Param("billingDate") LocalDate billingDate);

    /**
     * Verifica se cliente possui assinaturas ativas
     */
    @Query("""
        SELECT COUNT(s) > 0 FROM Subscription s 
        WHERE s.customerId = :customerId 
        AND s.status IN ('ACTIVE', 'TRIAL')
        """)
    boolean hasActiveSubscriptions(@Param("customerId") CustomerId customerId);

    /**
     * Conta total de assinaturas do cliente
     */
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.customerId = :customerId")
    long countByCustomerId(@Param("customerId") CustomerId customerId);

    /**
     * Busca assinaturas com período de graça expirado
     */
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'SUSPENDED' 
        AND s.gracePeriodEnd < :currentDate
        ORDER BY s.gracePeriodEnd ASC
        """)
    List<Subscription> findSubscriptionsWithExpiredGracePeriod(@Param("currentDate") LocalDate currentDate);

    /**
     * Busca assinaturas por plano
     */
    @Query("SELECT s FROM Subscription s WHERE s.planId = :planId ORDER BY s.createdAt DESC")
    List<Subscription> findByPlanId(@Param("planId") String planId);

    /**
     * Estatísticas de assinaturas por status
     */
    @Query("""
        SELECT s.status as status, COUNT(s) as count, SUM(s.monthlyPrice) as totalRevenue
        FROM Subscription s 
        GROUP BY s.status
        """)
    List<SubscriptionStats> getSubscriptionStatsByStatus();

    /**
     * Interface para projeção de estatísticas
     */
    interface SubscriptionStats {
        String getStatus();
        Long getCount();
        java.math.BigDecimal getTotalRevenue();
    }
}