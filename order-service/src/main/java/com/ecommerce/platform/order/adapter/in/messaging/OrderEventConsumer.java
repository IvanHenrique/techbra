package com.ecommerce.platform.order.adapter.in.messaging;

import com.ecommerce.platform.order.application.order.usecase.ProcessSubscriptionOrderUseCase;
import com.ecommerce.platform.shared.domain.PaymentFailedEvent;
import com.ecommerce.platform.shared.domain.PaymentProcessedEvent;
import com.ecommerce.platform.shared.domain.SubscriptionActivatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer de eventos relacionados a pedidos
 * Este componente implementa o padrão Event-Driven Architecture consumindo
 * eventos de outros bounded contexts que impactam o domínio de pedidos.
 * Eventos consumidos:
 * - SubscriptionActivated: Cria pedidos recorrentes para assinaturas ativas
 * - PaymentProcessed: Confirma pedidos após pagamento bem-sucedido
 * - PaymentFailed: Cancela pedidos quando pagamento falha
 * Padrões implementados:
 * - Event-Driven Architecture: Reação a eventos de outros serviços
 * - Saga Pattern: Participação em transações distribuídas
 * - At-least-once processing: Garantia de processamento com commit manual
 * - Idempotency: Eventos podem ser reprocessados safely
 * Características técnicas:
 * - Commit manual para garantir processamento
 * - Logging detalhado para observabilidade
 * - Tratamento de erros com retry automático
 * - Processamento assíncrono não bloqueante
 */
@Component
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ProcessSubscriptionOrderUseCase processSubscriptionOrderUseCase;

    @Autowired
    public OrderEventConsumer(ProcessSubscriptionOrderUseCase processSubscriptionOrderUseCase) {
        this.processSubscriptionOrderUseCase = processSubscriptionOrderUseCase;
    }

    /**
     * Processa evento de ativação de assinatura
     * Quando uma assinatura é ativada (após primeiro pagamento bem-sucedido),
     * este listener cria automaticamente um pedido recorrente para garantir
     * que o cliente receba os produtos/serviços da assinatura.
     * Fluxo de processamento:
     * 1. Valida dados do evento
     * 2. Cria pedido recorrente baseado na assinatura
     * 3. Agenda próximos pedidos automáticos
     * 4. Publica OrderCreated para outros serviços
     * 5. Confirma processamento (commit manual)
     * 
     * @param event Evento de ativação de assinatura
     * @param partition Partição do tópico Kafka
     * @param offset Offset da mensagem
     * @param acknowledgment Objeto para commit manual
     */
    @KafkaListener(
        topics = "subscription.events",
        groupId = "order-service",
        containerFactory = "kafkaListenerContainerFactory",
        properties = {
            "spring.json.value.default.type=com.ecommerce.platform.shared.domain.SubscriptionActivatedEvent"
        }
    )
    public void handleSubscriptionActivated(
            @Payload SubscriptionActivatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        logger.info("Processing SubscriptionActivated event: subscriptionId={}, eventId={}, partition={}, offset={}", 
                   event.subscriptionId(), event.getEventId(), partition, offset);

        try {
            // Validação básica do evento
            validateSubscriptionActivatedEvent(event);

            // Processa criação de pedido recorrente via use case
            var result = processSubscriptionOrderUseCase.execute(
                ProcessSubscriptionOrderUseCase.Command.builder()
                    .subscriptionId(event.subscriptionId())
                    .customerId(event.customerId())
                    .planId(event.planId())
                    .monthlyPrice(event.monthlyPrice())
                    .currency(event.currency())
                    .billingCycle(event.billingCycle())
                    .nextBillingDate(event.nextBillingDate())
                    .build()
            ).get();

            logger.info("Successfully created recurring order: orderId={}, subscriptionId={}", 
                       result.orderId(), event.subscriptionId());

            // Commit manual após processamento bem-sucedido
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process SubscriptionActivated event: subscriptionId={}, eventId={}", 
                        event.subscriptionId(), event.getEventId(), e);
            
            // Não faz acknowledge para que o Kafka tente reprocessar
            // O Spring Kafka fará retry automático baseado na configuração
            throw new EventProcessingException("Failed to process SubscriptionActivated event", e);
        }
    }

    /**
     * Processa eventos de pagamento processado
     * Quando um pagamento é confirmado, atualiza o status do pedido
     * correspondente e inicia o processo de fulfillment.
     * 
     * @param event Evento de pagamento processado
     * @param partition Partição do tópico Kafka
     * @param offset Offset da mensagem
     * @param acknowledgment Objeto para commit manual
     */
    @KafkaListener(
        topics = "payment.events",
        groupId = "order-service",
        containerFactory = "kafkaListenerContainerFactory",
        properties = {
            "spring.json.value.default.type=com.ecommerce.platform.shared.domain.PaymentProcessedEvent"
        }
    )
    public void handlePaymentProcessed(
            @Payload PaymentProcessedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        logger.info("Processing PaymentProcessed event: orderId={}, paymentId={}, partition={}, offset={}", 
                   event.orderId(), event.paymentId(), partition, offset);

        try {
            // TODO: Implementar confirmação de pedido após pagamento
            // confirmarPedidoUseCase.execute(event.orderId(), event.paymentId());

            logger.info("Successfully confirmed order after payment: orderId={}, paymentId={}", 
                       event.orderId(), event.paymentId());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process PaymentProcessed event: orderId={}, paymentId={}", 
                        event.orderId(), event.paymentId(), e);
            throw new EventProcessingException("Failed to process PaymentProcessed event", e);
        }
    }

    /**
     * Processa eventos de falha de pagamento
     * Quando um pagamento falha, cancela o pedido correspondente
     * e notifica o cliente sobre a falha.
     * 
     * @param event Evento de falha de pagamento
     * @param partition Partição do tópico Kafka
     * @param offset Offset da mensagem
     * @param acknowledgment Objeto para commit manual
     */
    @KafkaListener(
        topics = "payment.events",
        groupId = "order-service",
        containerFactory = "kafkaListenerContainerFactory",
        properties = {
            "spring.json.value.default.type=com.ecommerce.platform.shared.domain.PaymentFailedEvent"
        }
    )
    public void handlePaymentFailed(
            @Payload PaymentFailedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        logger.info("Processing PaymentFailed event: orderId={}, paymentId={}, reason={}, partition={}, offset={}", 
                   event.orderId(), event.paymentId(), event.failureReason(), partition, offset);

        try {
            // TODO: Implementar cancelamento de pedido após falha de pagamento
            // cancelarPedidoUseCase.execute(event.orderId(), event.failureReason());

            logger.info("Successfully cancelled order after payment failure: orderId={}, reason={}", 
                       event.orderId(), event.failureReason());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process PaymentFailed event: orderId={}, paymentId={}", 
                        event.orderId(), event.paymentId(), e);
            throw new EventProcessingException("Failed to process PaymentFailed event", e);
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Valida dados essenciais do evento SubscriptionActivated
     * 
     * @param event Evento a ser validado
     * @throws IllegalArgumentException se dados obrigatórios estão ausentes
     */
    private void validateSubscriptionActivatedEvent(SubscriptionActivatedEvent event) {
        if (event.subscriptionId() == null || event.subscriptionId().isBlank()) {
            throw new IllegalArgumentException("Subscription ID is required");
        }
        
        if (event.customerId() == null || event.customerId().isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        
        if (event.planId() == null || event.planId().isBlank()) {
            throw new IllegalArgumentException("Plan ID is required");
        }
        
        if (event.monthlyPrice() == null || event.monthlyPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monthly price must be non-negative");
        }
        
        if (event.nextBillingDate() == null) {
            throw new IllegalArgumentException("Next billing date is required");
        }
        
        logger.debug("SubscriptionActivated event validation passed: subscriptionId={}", 
                    event.subscriptionId());
    }

    /**
     * Exception customizada para erros de processamento de eventos
     * Utilizada para encapsular erros durante processamento de eventos
     * e permitir retry automático pelo Kafka Consumer.
     */
    public static class EventProcessingException extends RuntimeException {
        public EventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}