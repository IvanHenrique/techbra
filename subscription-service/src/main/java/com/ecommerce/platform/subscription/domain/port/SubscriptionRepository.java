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
 * Representa a abstração do repositório no padrão Hexagonal Architecture.
 * Características importantes:
 * - Interface pura (sem dependências de framework)
 * - Linguagem ubíqua do domínio de assinaturas
 * - Métodos expressivos e orientados ao negócio
 * - Não vaza detalhes de implementação
 * Esta interface será implementada pelo adapter de infrastructure
 * (ex: SubscriptionJpaRepository) mantendo o domínio isolado.
 */
public interface SubscriptionRepository {
    
    /**
     * Salva uma assinatura (insert ou update)
     * 
     * @param subscription Assinatura a ser salva
     * @return Assinatura salva com dados atualizados
     */
    Subscription save(Subscription subscription);
    
    /**
     * Busca assinatura por ID
     * 
     * @param subscriptionId ID único da assinatura
     * @return Optional contendo a assinatura se encontrada
     */
    Optional<Subscription> findById(UUID subscriptionId);
    
    /**
     * Busca todas as assinaturas de um cliente
     * Útil para dashboard do cliente e gestão
     * 
     * @param customerId ID do cliente
     * @return Lista de assinaturas do cliente (ordenadas por data de criação DESC)
     */
    List<Subscription> findByCustomerId(CustomerId customerId);
    
    /**
     * Busca assinaturas por status
     * Útil para processamento em lote e monitoramento
     * 
     * @param status Status das assinaturas buscadas
     * @return Lista de assinaturas com o status especificado
     */
    List<Subscription> findByStatus(String status);
    
    /**
     * Busca assinaturas ativas que precisam ser cobradas
     * Usado pelo scheduler para processar cobranças automáticas
     * 
     * @param billingDate Data de referência para cobrança (hoje)
     * @return Lista de assinaturas que precisam ser cobradas
     */
    List<Subscription> findActiveSubscriptionsDueForBilling(LocalDate billingDate);
    
    /**
     * Busca assinaturas em período de graça que expiraram
     * Para cancelamento automático após múltiplas falhas de pagamento
     * 
     * @param currentDate Data atual
     * @return Lista de assinaturas com período de graça expirado
     */
    List<Subscription> findSubscriptionsWithExpiredGracePeriod(LocalDate currentDate);
    
    /**
     * Busca assinaturas por plano
     * Útil para relatórios e análises por produto
     * 
     * @param planId ID do plano
     * @return Lista de assinaturas do plano especificado
     */
    List<Subscription> findByPlanId(String planId);
    
    /**
     * Conta assinaturas ativas de um cliente
     * Para validações de negócio (ex: limite de assinaturas por cliente)
     * 
     * @param customerId ID do cliente
     * @return Número de assinaturas ativas do cliente
     */
    long countActiveSubscriptionsByCustomerId(CustomerId customerId);
    
    /**
     * Verifica se existe assinatura ativa para um plano específico do cliente
     * Evita assinaturas duplicadas do mesmo plano
     * 
     * @param customerId ID do cliente
     * @param planId ID do plano
     * @return true se já existe assinatura ativa deste plano para o cliente
     */
    boolean existsActiveSubscriptionForCustomerAndPlan(CustomerId customerId, String planId);
    
    /**
     * Busca assinaturas criadas em um período
     * Para relatórios e analytics
     * 
     * @param startDate Data inicial
     * @param endDate Data final
     * @return Lista de assinaturas criadas no período
     */
    List<Subscription> findSubscriptionsCreatedBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Remove assinatura por ID
     * CUIDADO: Usar apenas em casos específicos (ex: dados de teste)
     * Em produção, prefira cancelamento lógico
     * 
     * @param subscriptionId ID da assinatura a ser removida
     */
    void deleteById(UUID subscriptionId);
    
    /**
     * Verifica se existe assinatura com o ID
     * 
     * @param subscriptionId ID da assinatura
     * @return true se a assinatura existe
     */
    boolean existsById(UUID subscriptionId);
}