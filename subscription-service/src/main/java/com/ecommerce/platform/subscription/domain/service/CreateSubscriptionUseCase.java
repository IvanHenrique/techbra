package com.ecommerce.platform.subscription.domain.service;

import com.ecommerce.platform.shared.domain.DomainEvent;
import com.ecommerce.platform.shared.domain.SubscriptionCreatedEvent;
import com.ecommerce.platform.subscription.domain.model.Subscription;
import com.ecommerce.platform.subscription.domain.port.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CreateSubscriptionUseCase - Caso de Uso do Domínio
 * Implementa a lógica de criação de assinaturas seguindo princípios DDD:
 * - Use Case puro sem dependências de framework (exceto anotações)
 * - Orquestra operações do domínio de assinaturas
 * - Coordena chamadas para repositories e services externos
 * - Integra com billing gateway para cobrança recorrente
 * - Publica eventos de domínio
 * - Mantém transações consistentes
 * Padrões aplicados:
 * - Command Pattern: Encapsula operação de criação
 * - Transaction Script: Para coordenação transacional
 * - Domain Events: Para comunicação entre contextos
 * - Saga Pattern: Coordenação com billing system
 */
@Service
@Transactional
public class CreateSubscriptionUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(CreateSubscriptionUseCase.class);
    
    private final SubscriptionRepository subscriptionRepository;
    private final BillingGateway billingGateway;
    private final EventPublisher eventPublisher;
    
    public CreateSubscriptionUseCase(SubscriptionRepository subscriptionRepository,
                                   BillingGateway billingGateway,
                                   EventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.billingGateway = billingGateway;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Executa criação de assinatura
     * 
     * @param command Comando com dados da assinatura
     * @return Resultado da operação
     */
    public CreateSubscriptionResult execute(CreateSubscriptionCommand command) {
        logger.info("Iniciando criação de assinatura para cliente: {}", 
            command != null ? command.customerId() : "null");
        
        try {
            // 1. Validações de negócio
            validateCommand(command);
            
            // 2. Verificar se cliente já tem assinatura ativa deste plano
            if (subscriptionRepository.existsActiveSubscriptionForCustomerAndPlan(
                    command.customerId(), command.planId())) {
                return CreateSubscriptionResult.failure("Cliente já possui assinatura ativa deste plano");
            }
            
            // 3. Criar a assinatura
            Subscription subscription = Subscription.create(
                command.customerId(),
                command.customerEmail(),
                command.planId(),
                command.billingCycle(),
                command.monthlyAmount()
            );
            
            // 4. Salvar a assinatura
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            logger.info("Assinatura criada com sucesso: {}", savedSubscription.getId());
            
            // 5. Agendar cobrança recorrente no billing gateway
            BillingScheduleResult billingResult = scheduleBilling(savedSubscription, command.paymentMethodToken());
            
            if (billingResult.success()) {
                // 6a. Billing agendado com sucesso - ativar assinatura
                savedSubscription.activate();
                savedSubscription = subscriptionRepository.save(savedSubscription);
                
                logger.info("Cobrança agendada e assinatura ativada: {}", savedSubscription.getId());
                
                // 7. Publicar evento de domínio
                publishSubscriptionCreatedEvent(savedSubscription);
                
                return CreateSubscriptionResult.success(savedSubscription);
                
            } else {
                // 6b. Falha no agendamento de billing - cancelar assinatura
                logger.warn("Falha ao agendar cobrança para assinatura {}: {}", 
                    savedSubscription.getId(), billingResult.message());
                
                savedSubscription.cancel();
                subscriptionRepository.save(savedSubscription);
                
                return CreateSubscriptionResult.failure("Erro ao configurar cobrança: " + billingResult.message());
            }
            
        } catch (Exception e) {
            logger.error("Erro ao criar assinatura: {}", e.getMessage(), e);
            return CreateSubscriptionResult.failure(e.getMessage());
        }
    }
    
    /**
     * Valida comando de criação de assinatura
     */
    private void validateCommand(CreateSubscriptionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Comando não pode ser null");
        }
        
        // Validar limite de assinaturas por cliente
        long activeSubscriptions = subscriptionRepository.countActiveSubscriptionsByCustomerId(command.customerId());
        if (activeSubscriptions >= 10) { // Limite de negócio: máximo 10 assinaturas ativas por cliente
            throw new IllegalStateException("Cliente possui muitas assinaturas ativas");
        }
    }
    
    /**
     * Agenda cobrança recorrente no billing gateway
     */
    private BillingScheduleResult scheduleBilling(Subscription subscription, String paymentMethodToken) {
        try {
            BillingScheduleRequest request = new BillingScheduleRequest(
                subscription.getId(),
                subscription.getCustomerId(),
                subscription.getCustomerEmail(),
                subscription.calculateBillingAmount(),
                subscription.getBillingCycle(),
                subscription.getNextBillingDate(),
                paymentMethodToken
            );
            
            return billingGateway.scheduleBilling(request);
            
        } catch (Exception e) {
            logger.error("Erro ao agendar cobrança para assinatura {}: {}", 
                subscription.getId(), e.getMessage(), e);
            return BillingScheduleResult.failure("Erro interno ao agendar cobrança");
        }
    }
    
    /**
     * Publica evento de assinatura criada
     */
    private void publishSubscriptionCreatedEvent(Subscription subscription) {
        try {
            DomainEvent event = SubscriptionCreatedEvent.of(
                subscription.getId(),
                subscription.getCustomerId().value(),
                subscription.getPlanId(),
                subscription.getMonthlyAmount().amount()
            );
            
            eventPublisher.publish(event);
            logger.info("Evento SubscriptionCreated publicado para assinatura: {}", subscription.getId());
            
        } catch (Exception e) {
            logger.error("Erro ao publicar evento para assinatura {}: {}", 
                subscription.getId(), e.getMessage(), e);
            // Não propagar erro - evento é importante mas não crítico para a operação
        }
    }
}