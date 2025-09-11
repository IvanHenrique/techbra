package com.ecommerce.platform.subscription.application.subscription.usecase;

import com.ecommerce.platform.shared.domain.EventPublisher;
import com.ecommerce.platform.shared.domain.SubscriptionCreatedEvent;
import com.ecommerce.platform.shared.domain.ValueObjects.CustomerEmail;
import com.ecommerce.platform.shared.domain.ValueObjects.CustomerId;
import com.ecommerce.platform.shared.domain.ValueObjects.Money;
import com.ecommerce.platform.subscription.application.subscription.command.CreateSubscriptionCommand;
import com.ecommerce.platform.subscription.application.subscription.result.CreateSubscriptionResult;
import com.ecommerce.platform.subscription.domain.model.Subscription;
import com.ecommerce.platform.subscription.domain.port.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * CreateSubscriptionUseCase - Caso de uso para criação de assinaturas
 * Responsável por orquestrar a criação de novas assinaturas no sistema.
 */
@Service
@Transactional
public class CreateSubscriptionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateSubscriptionUseCase.class);

    private final SubscriptionRepository subscriptionRepository;
    private final EventPublisher eventPublisher;

    public CreateSubscriptionUseCase(SubscriptionRepository subscriptionRepository,
                                     EventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Executa a criação de uma nova assinatura
     */
    public CreateSubscriptionResult execute(CreateSubscriptionCommand command) {
        logger.info("Iniciando criação de assinatura para cliente: {}", command.customerId());

        try {
            // 1. Validar se cliente já possui assinatura ativa do mesmo plano
            validateNoDuplicateActiveSubscription(command.customerId(), command.planId());

            // 2. Criar agregado Subscription
            Subscription subscription = Subscription.create(
                    CustomerId.of(command.customerId()),
                    CustomerEmail.of(command.customerEmail()),
                    command.planId(),
                    command.billingCycle(),
                    Money.of(command.monthlyPrice(), "BRL"),
                    command.trialPeriodDays(),
                    command.paymentMethodId()
            );

            // 3. Persistir assinatura
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            logger.debug("Assinatura persistida com ID: {}", savedSubscription.getId());

            // 4. Publicar evento SubscriptionCreated
            publishSubscriptionCreatedEvent(savedSubscription);

            logger.info("Assinatura criada com sucesso: {}", savedSubscription.getId());
            return CreateSubscriptionResult.success(savedSubscription);

        } catch (Exception e) {
            logger.error("Erro ao criar assinatura para cliente: {}", command.customerId(), e);
            return CreateSubscriptionResult.failure(e.getMessage());
        }
    }

    /**
     * Valida se cliente já possui assinatura ativa do mesmo plano
     */
    private void validateNoDuplicateActiveSubscription(String customerId, String planId) {
        var existingSubscriptions = subscriptionRepository.findByCustomerId(CustomerId.of(customerId));

        boolean hasActiveSubscription = existingSubscriptions.stream()
                .anyMatch(sub -> sub.getPlanId().equals(planId) &&
                        (sub.isActive() || sub.isTrial()));

        if (hasActiveSubscription) {
            throw new IllegalStateException("Cliente já possui assinatura ativa deste plano");
        }
    }

    /**
     * Publica evento de assinatura criada
     */
    private void publishSubscriptionCreatedEvent(Subscription subscription) {
        try {
            SubscriptionCreatedEvent event = SubscriptionCreatedEvent.create(
                    subscription.getId().toString(),
                    subscription.getCustomerId().value().toString(),
                    subscription.getPlanId(),
                    "Plano Padrão",
                    subscription.getMonthlyPrice().amount(),
                    subscription.getMonthlyPrice().currency(),
                    subscription.getBillingCycle().name(),
                    subscription.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
                    subscription.getNextBillingDate().atStartOfDay(),
                    subscription.getTrialPeriodDays(),
                    subscription.getPaymentMethodId()
            );

            // Usar o CompletableFuture corretamente
            CompletableFuture<Void> future = eventPublisher.publish(event);

            future.thenRun(() -> {
                logger.debug("Evento SubscriptionCreated publicado com sucesso: {}", event.eventId());
            }).exceptionally(throwable -> {
                logger.error("Erro ao publicar evento SubscriptionCreated: {}", event.eventId(), throwable);
                return null;
            });

        } catch (Exception e) {
            logger.error("Erro ao criar/publicar evento SubscriptionCreated", e);
            // Não propagar erro - assinatura já foi criada
        }
    }
}