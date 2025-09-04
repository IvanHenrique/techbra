package com.ecommerce.platform.subscription.domain.port;

import java.util.UUID;

/**
 * BillingGateway - Port (Interface) do Domínio
 * Define o contrato para integração com sistemas de cobrança recorrente
 * sem depender de implementação específica (Stripe, PayPal, etc).
 * Responsabilidades:
 * - Agendar cobranças recorrentes automáticas
 * - Processar cobranças de assinaturas
 * - Gerenciar inadimplência e retry de pagamentos
 * - Abstrair complexidades de diferentes billing systems
 */
public interface BillingGateway {
    
    /**
     * Agenda cobrança recorrente para uma assinatura
     * 
     * @param request Dados para agendamento da cobrança
     * @return Resultado do agendamento
     */
    BillingScheduleResult scheduleBilling(BillingScheduleRequest request);
    
    /**
     * Processa cobrança imediata de uma assinatura
     * 
     * @param request Dados da cobrança
     * @return Resultado do processamento
     */
    BillingResult processBilling(BillingRequest request);
    
    /**
     * Cancela agendamento de cobrança recorrente
     * 
     * @param subscriptionId ID da assinatura
     * @return Resultado do cancelamento
     */
    BillingCancellationResult cancelBilling(UUID subscriptionId);
    
    /**
     * Atualiza dados de cobrança de uma assinatura
     * 
     * @param request Novos dados de cobrança
     * @return Resultado da atualização
     */
    BillingUpdateResult updateBilling(BillingUpdateRequest request);
    
    /**
     * Consulta status de cobrança recorrente
     * 
     * @param subscriptionId ID da assinatura
     * @return Status atual da cobrança
     */
    BillingStatus getBillingStatus(UUID subscriptionId);
}