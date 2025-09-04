package com.ecommerce.platform.subscription.adapter.out.database;

import com.ecommerce.platform.subscription.domain.model.Subscription;
import com.ecommerce.platform.subscription.domain.port.SubscriptionRepository;
import com.ecommerce.platform.shared.domain.ValueObjects.CustomerId;
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
 * Atua como adapter na arquitetura hexagonal, convertendo
 * operações de domínio em queries de banco de dados.
 * Responsabilidades:
 * - Implementar todas as operações definidas no port
 * - Converter entre entidades JPA e objetos de domínio
 * - Otimizar queries para performance de billing
 * - Manter consistência transacional
 * - Prover queries específicas para processamento recorrente
 */
@Repository
public interface SubscriptionJpaRepository extends JpaRepository<Subscription, UUID>, SubscriptionRepository {
    
    /**
     * Busca assinaturas por ID do cliente
     * Ordenadas por data de criação (mais recentes primeiro)
     */
    @Query("SELECT s FROM Subscription s WHERE s.customerId.value = :customerId ORDER BY s.createdAt DESC")
    List<Subscription> findByCustomerId(@Param("customerId") CustomerId customerId);
    
    /**
     * Busca assinaturas por status
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = :status")
    List<Subscription> findByStatus(@Param("status") String status);
    
    /**
     * Busca assinaturas ativas que precisam ser cobradas
     * Inclui assinaturas com status ACTIVE e nextBillingDate <= data informada
     */
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'ACTIVE' 
        AND s.nextBillingDate <= :billingDate
        ORDER BY s.nextBillingDate ASC
        """)
    List<Subscription> findActiveSubscriptionsDueForBilling(@Param("billingDate") LocalDate billingDate);
    
    /**
     * Busca assinaturas com período de graça expirado
     * Para cancelamento automático após múltiplas falhas
     */
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'PAST_DUE' 
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
     * Conta assinaturas ativas por cliente
     */
    @Query("""
        SELECT COUNT(s) FROM Subscription s 
        WHERE s.customerId.value = :customerId 
        AND s.status = 'ACTIVE'
        """)
    long countActiveSubscriptionsByCustomerId(@Param("customerId") CustomerId customerId);
    
    /**
     * Verifica se existe assinatura ativa para cliente e plano específicos
     */
    @Query("""
        SELECT COUNT(s) > 0 FROM Subscription s 
        WHERE s.customerId.value = :customerId 
        AND s.planId = :planId 
        AND s.status = 'ACTIVE'
        """)
    boolean existsActiveSubscriptionForCustomerAndPlan(@Param("customerId") CustomerId customerId, 
                                                       @Param("planId") String planId);
    
    /**
     * Busca assinaturas criadas em um período
     */
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.startDate BETWEEN :startDate AND :endDate
        ORDER BY s.startDate DESC
        """)
    List<Subscription> findSubscriptionsCreatedBetween(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * Queries adicionais para relatórios e analytics

     * Busca assinaturas que vencem nos próximos X dias
     * Para notificações proativas
     */
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'ACTIVE' 
        AND s.nextBillingDate BETWEEN :startDate AND :endDate
        ORDER BY s.nextBillingDate ASC
        """)
    List<Subscription> findSubscriptionsDueInPeriod(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
    
    /**
     * Estatísticas de assinaturas por status
     */
    @Query("""
        SELECT s.status as status, COUNT(s) as count, SUM(s.monthlyAmount.amount) as totalRevenue
        FROM Subscription s 
        GROUP BY s.status
        """)
    List<SubscriptionStatusStats> getSubscriptionStatsByStatus();
    
    /**
     * Top planos mais populares
     */
    @Query("""
        SELECT s.planId as planId, 
               COUNT(s) as subscriptionCount,
               SUM(s.monthlyAmount.amount) as totalRevenue
        FROM Subscription s
        WHERE s.status = 'ACTIVE'
        GROUP BY s.planId
        ORDER BY subscriptionCount DESC
        """)
    List<PlanPopularityStats> getTopPlans();
    
    /**
     * Assinaturas com falhas recorrentes de pagamento
     * Para análise de inadimplência
     */
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'PAST_DUE' 
        AND s.failedPaymentAttempts >= :minFailures
        ORDER BY s.failedPaymentAttempts DESC, s.gracePeriodEnd ASC
        """)
    List<Subscription> findSubscriptionsWithMultiplePaymentFailures(@Param("minFailures") int minFailures);
    
    /**
     * MRR (Monthly Recurring Revenue) por período
     */
    @Query("""
        SELECT SUM(
            CASE s.billingCycle 
                WHEN 'MONTHLY' THEN s.monthlyAmount.amount
                WHEN 'QUARTERLY' THEN s.monthlyAmount.amount
                WHEN 'YEARLY' THEN s.monthlyAmount.amount
            END
        ) as mrr
        FROM Subscription s 
        WHERE s.status = 'ACTIVE'
        """)
    java.math.BigDecimal calculateMonthlyRecurringRevenue();
    
    /**
     * Assinaturas por ciclo de cobrança
     */
    @Query("""
        SELECT s.billingCycle as cycle, COUNT(s) as count
        FROM Subscription s 
        WHERE s.status = 'ACTIVE'
        GROUP BY s.billingCycle
        """)
    List<BillingCycleStats> getSubscriptionsByBillingCycle();
    
    /**
     * Churn rate - assinaturas canceladas no período
     */
    @Query("""
        SELECT COUNT(s) FROM Subscription s 
        WHERE s.status = 'CANCELLED' 
        AND s.endDate BETWEEN :startDate AND :endDate
        """)
    long countCancelledSubscriptionsInPeriod(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
}

/**
 * Projection para estatísticas de status
 */
interface SubscriptionStatusStats {
    String getStatus();
    Long getCount();
    java.math.BigDecimal getTotalRevenue();
}

/**
 * Projection para estatísticas de planos
 */
interface PlanPopularityStats {
    String getPlanId();
    Long getSubscriptionCount();
    java.math.BigDecimal getTotalRevenue();
}

/**
 * Projection para estatísticas de ciclo de cobrança
 */
interface BillingCycleStats {
    String getCycle();
    Long getCount();
}
