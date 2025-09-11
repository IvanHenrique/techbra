package com.ecommerce.platform.subscription.application.subscription.usecase;

import com.ecommerce.platform.shared.domain.EventPublisher;
import com.ecommerce.platform.subscription.application.subscription.result.ProcessBillingResult;
import com.ecommerce.platform.subscription.domain.model.Subscription;
import com.ecommerce.platform.subscription.domain.port.SubscriptionRepository;
import com.ecommerce.platform.subscription.domain.port.BillingGateway;
import com.ecommerce.platform.subscription.application.subscription.request.BillingRequest;
import com.ecommerce.platform.subscription.application.subscription.result.BillingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ProcessBillingUseCase - Caso de Uso do Domínio
 * Gerencia o processamento de cobranças recorrentes de assinaturas:
 * - Identifica assinaturas que precisam ser cobradas
 * - Processa cobranças automáticas via billing gateway
 * - Gerencia falhas de pagamento e inadimplência
 * - Atualiza status das assinaturas
 * - Publica eventos para outros contextos
 * Usado principalmente por jobs agendados (Quartz Scheduler)
 * para processamento automático de cobranças.
 */
@Service
@Transactional
public class ProcessBillingUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessBillingUseCase.class);
    
    private final SubscriptionRepository subscriptionRepository;
    private final BillingGateway billingGateway;
    private final EventPublisher eventPublisher;
    
    public ProcessBillingUseCase(SubscriptionRepository subscriptionRepository,
                               BillingGateway billingGateway,
                               EventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.billingGateway = billingGateway;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Processa todas as assinaturas que vencem hoje
     * 
     * Método principal usado pelo scheduler automático
     */
    public ProcessBillingResult processScheduledBilling() {
        logger.info("Iniciando processamento de cobranças agendadas para hoje");
        
        LocalDate today = LocalDate.now();
        List<Subscription> subscriptionsDue = subscriptionRepository
            .findActiveSubscriptionsDueForBilling(today);
        
        logger.info("Encontradas {} assinaturas para cobrança hoje", subscriptionsDue.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (Subscription subscription : subscriptionsDue) {
            try {
                ProcessBillingResult result = processSingleSubscriptionBilling(subscription.getId());
                
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failureCount++;
                }
                
            } catch (Exception e) {
                logger.error("Erro ao processar cobrança da assinatura {}: {}", 
                    subscription.getId(), e.getMessage(), e);
                failureCount++;
            }
        }
        
        logger.info("Processamento concluído: {} sucessos, {} falhas", successCount, failureCount);
        
        return new ProcessBillingResult(
            true, 
            String.format("Processadas %d assinaturas: %d sucessos, %d falhas", 
                subscriptionsDue.size(), successCount, failureCount),
            successCount,
            failureCount
        );
    }
    
    /**
     * Processa cobrança de uma assinatura específica
     * 
     * @param subscriptionId ID da assinatura
     * @return Resultado do processamento
     */
    public ProcessBillingResult processSingleSubscriptionBilling(UUID subscriptionId) {
        logger.info("Processando cobrança da assinatura: {}", subscriptionId);
        
        try {
            // 1. Buscar assinatura
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
            if (subscriptionOpt.isEmpty()) {
                return ProcessBillingResult.failure("Assinatura não encontrada");
            }
            
            Subscription subscription = subscriptionOpt.get();
            
            // 2. Validar se precisa ser cobrada
            if (!subscription.needsBilling()) {
                return ProcessBillingResult.failure("Assinatura não precisa ser cobrada no momento");
            }
            
            // 3. Processar cobrança via billing gateway
            BillingRequest billingRequest = new BillingRequest(
                subscription.getId(),
                subscription.getCustomerId(),
                subscription.calculateBillingAmount(),
                "stored_payment_method", // Em prod seria recuperado do customer vault
                false // não é retry
            );
            
            BillingResult billingResult = billingGateway.processBilling(billingRequest);
            
            if (billingResult.isSuccess()) {
                // 4a. Cobrança bem-sucedida
                subscription.processSuccessfulBilling();
                subscriptionRepository.save(subscription);
                
                logger.info("Cobrança processada com sucesso para assinatura: {}", subscriptionId);
                publishBillingSuccessEvent(subscription, billingResult);
                
                return ProcessBillingResult.success("Cobrança processada com sucesso");
                
            } else {
                // 4b. Cobrança falhou
                subscription.processFailedBilling();
                subscriptionRepository.save(subscription);
                
                logger.warn("Falha na cobrança da assinatura {}: {}", 
                    subscriptionId, billingResult.message());
                publishBillingFailureEvent(subscription, billingResult);
                
                return ProcessBillingResult.failure("Falha na cobrança: " + billingResult.message());
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar cobrança da assinatura {}: {}", 
                subscriptionId, e.getMessage(), e);
            return ProcessBillingResult.failure("Erro interno: " + e.getMessage());
        }
    }
    
    /**
     * Processa retry de cobranças falhadas
     */
    public ProcessBillingResult processFailedBillingRetries() {
        logger.info("Processando retry de cobranças falhadas");
        
        List<Subscription> pastDueSubscriptions = subscriptionRepository
            .findByStatus("PAST_DUE");
        
        int retrySuccessCount = 0;
        int retryFailureCount = 0;
        
        for (Subscription subscription : pastDueSubscriptions) {
            // Só tentar retry se ainda está no período de graça
            if (!subscription.isInGracePeriod()) {
                continue;
            }
            
            try {
                ProcessBillingResult result = retryFailedBilling(subscription.getId());
                
                if (result.isSuccess()) {
                    retrySuccessCount++;
                } else {
                    retryFailureCount++;
                }
                
            } catch (Exception e) {
                logger.error("Erro no retry da assinatura {}: {}", 
                    subscription.getId(), e.getMessage(), e);
                retryFailureCount++;
            }
        }
        
        logger.info("Retry concluído: {} sucessos, {} falhas", retrySuccessCount, retryFailureCount);
        
        return new ProcessBillingResult(
            true,
            String.format("Retry de %d assinaturas: %d sucessos, %d falhas",
                pastDueSubscriptions.size(), retrySuccessCount, retryFailureCount),
            retrySuccessCount,
            retryFailureCount
        );
    }
    
    /**
     * Faz retry de cobrança falhada
     */
    private ProcessBillingResult retryFailedBilling(UUID subscriptionId) {
        logger.info("Tentando retry de cobrança para assinatura: {}", subscriptionId);
        
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        if (subscriptionOpt.isEmpty()) {
            return ProcessBillingResult.failure("Assinatura não encontrada");
        }
        
        Subscription subscription = subscriptionOpt.get();
        
        // Criar request de retry
        BillingRequest retryRequest = new BillingRequest(
            subscription.getId(),
            subscription.getCustomerId(),
            subscription.calculateBillingAmount(),
            "stored_payment_method", // Em prod seria recuperado do customer vault
            true // é retry
        );
        
        BillingResult billingResult = billingGateway.processBilling(retryRequest);
        
        if (billingResult.isSuccess()) {
            subscription.processSuccessfulBilling();
            subscriptionRepository.save(subscription);
            
            logger.info("Retry de cobrança bem-sucedido para assinatura: {}", subscriptionId);
            publishBillingSuccessEvent(subscription, billingResult);
            
            return ProcessBillingResult.success("Retry bem-sucedido");
            
        } else {
            subscription.processFailedBilling();
            subscriptionRepository.save(subscription);
            
            logger.warn("Retry de cobrança falhou para assinatura {}: {}", 
                subscriptionId, billingResult.message());
            
            return ProcessBillingResult.failure("Retry falhou: " + billingResult.message());
        }
    }
    
    /**
     * Cancela automaticamente assinaturas com período de graça expirado
     */
    public ProcessBillingResult processExpiredGracePeriods() {
        logger.info("Processando assinaturas com período de graça expirado");
        
        LocalDate today = LocalDate.now();
        List<Subscription> expiredSubscriptions = subscriptionRepository
            .findSubscriptionsWithExpiredGracePeriod(today);
        
        int cancelledCount = 0;
        
        for (Subscription subscription : expiredSubscriptions) {
            try {
                subscription.cancel();
                subscriptionRepository.save(subscription);
                
                logger.info("Assinatura {} cancelada automaticamente por período de graça expirado", 
                    subscription.getId());
                
                publishSubscriptionCancelledEvent(subscription, "Período de graça expirado");
                cancelledCount++;
                
            } catch (Exception e) {
                logger.error("Erro ao cancelar assinatura {}: {}", 
                    subscription.getId(), e.getMessage(), e);
            }
        }
        
        logger.info("Canceladas {} assinaturas por período de graça expirado", cancelledCount);
        
        return new ProcessBillingResult(
            true,
            String.format("Canceladas %d assinaturas por período de graça expirado", cancelledCount),
            cancelledCount,
            0
        );
    }
    
    // Métodos privados para publicação de eventos
    private void publishBillingSuccessEvent(Subscription subscription, BillingResult result) {
        // TODO: Implementar evento BillingProcessed quando necessário
        logger.debug("Evento BillingSuccess seria publicado para assinatura: {}", subscription.getId());
    }
    
    private void publishBillingFailureEvent(Subscription subscription, BillingResult result) {
        // TODO: Implementar evento BillingFailed quando necessário
        logger.debug("Evento BillingFailure seria publicado para assinatura: {}", subscription.getId());
    }
    
    private void publishSubscriptionCancelledEvent(Subscription subscription, String reason) {
        // TODO: Implementar evento SubscriptionCancelled quando necessário
        logger.debug("Evento SubscriptionCancelled seria publicado para assinatura: {} com motivo: {}", 
            subscription.getId(), reason);
    }
}