package com.ecommerce.platform.subscription.domain.port;

import com.ecommerce.platform.subscription.domain.model.Subscription;
import com.ecommerce.platform.shared.domain.ValueObjects.CustomerId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SubscriptionRepository - Port (Interface) do Domínio
 * Define o contrato para persistência de assinaturas sem depender de tecnologia específica.
 */
public interface SubscriptionRepository {

    /**
     * Salva uma assinatura (insert ou update)
     */
    Subscription save(Subscription subscription);

    /**
     * Busca assinatura por ID
     */
    Optional<Subscription> findById(UUID subscriptionId);

    /**
     * Busca todas as assinaturas de um cliente
     */
    List<Subscription> findByCustomerId(CustomerId customerId);

    /**
     * Busca assinaturas por status
     */
    List<Subscription> findByStatus(String status);

    /**
     * Busca assinaturas ativas que precisam ser cobradas
     */
    List<Subscription> findActiveSubscriptionsDueForBilling(LocalDate billingDate);

    /**
     * Verifica se cliente possui assinaturas ativas
     */
    boolean hasActiveSubscriptions(CustomerId customerId);

    /**
     * Conta total de assinaturas do cliente
     */
    long countByCustomerId(CustomerId customerId);

    /**
     * Remove assinatura (soft delete)
     */
    void deleteById(UUID subscriptionId);

    List<Subscription> findSubscriptionsWithExpiredGracePeriod(LocalDate currentDate);
}