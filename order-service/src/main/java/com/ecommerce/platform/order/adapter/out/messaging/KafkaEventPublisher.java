package com.ecommerce.platform.order.adapter.out.messaging;

import com.ecommerce.platform.order.domain.port.EventPublisher;
import com.ecommerce.platform.shared.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * KafkaEventPublisher - Adapter de Saída (Messaging)
 * Implementa o port EventPublisher usando Apache Kafka.
 * Responsável por publicar eventos de domínio para outros bounded contexts.
 * Características importantes:
 * - Publicação assíncrona com callbacks
 * - Retry automático em caso de falha
 * - Logging detalhado para auditoria
 * - Mapeamento de eventos para tópicos específicos
 * - Tratamento de erros robusto
 * Padrões aplicados:
 * - Adapter Pattern: Adapta Kafka para domínio
 * - Publisher-Subscriber: Comunicação assíncrona
 * - Retry Pattern: Resiliência em falhas temporárias
 */
@Component
public class KafkaEventPublisher implements EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // Configuração de tópicos
    private static final String ORDER_EVENTS_TOPIC = "order.events";
    private static final String PAYMENT_EVENTS_TOPIC = "payment.events";
    
    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Verifica se o tópico existe (utilitário)
     */
    public boolean isTopicAvailable(String topic) {
        try {
            // Implementação simplificada - em produção usaria AdminClient
            return true;
        } catch (Exception e) {
            logger.warn("Erro ao verificar disponibilidade do tópico {}: {}", topic, e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtém métricas de publicação (futuro)
     */
    public PublishingMetrics getMetrics() {
        // TODO: Implementar coleta de métricas
        return new PublishingMetrics(0L, 0L, 0L);
    }

    /**
     * Publica evento de domínio no tópico apropriado
     */
    @Override
    public void publish(DomainEvent event) {
        try {
            String topic = getTopicForEvent(event);
            String key = generateMessageKey(event);

            logger.info("Publicando evento {} no tópico {}",
                    event.getClass().getSimpleName(), topic);

            // Envio assíncrono com callback
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    handleSuccessfulPublish(event, result);
                } else {
                    handleFailedPublish(event, throwable);
                }
            });

        } catch (Exception e) {
            logger.error("Erro ao publicar evento {}: {}",
                    event.getClass().getSimpleName(), e.getMessage(), e);
            throw new EventPublishException("Falha na publicação do evento", e);
        }
    }

    /**
     * Publica múltiplos eventos
     */
    @Override
    public void publishAll(DomainEvent... events) {
        if (events == null || events.length == 0) {
            logger.debug("Nenhum evento para publicar");
            return;
        }

        logger.info("Publicando {} eventos em lote", events.length);

        for (DomainEvent event : events) {
            try {
                publish(event);
            } catch (Exception e) {
                logger.error("Erro ao publicar evento {} em lote: {}",
                        event.getClass().getSimpleName(), e.getMessage(), e);
                // Continua processando outros eventos mesmo se um falhar
            }
        }
    }

    /**
     * Publica evento com retry automático
     */
    @Override
    @Retryable(
            retryFor = {EventPublishException.class, RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishWithRetry(DomainEvent event, int maxRetries) {
        logger.info("Publicando evento {} com retry (max: {})",
                event.getClass().getSimpleName(), maxRetries);

        try {
            publish(event);
        } catch (Exception e) {
            logger.warn("Tentativa de publicação falhou para evento {}: {}",
                    event.getClass().getSimpleName(), e.getMessage());
            throw new EventPublishException("Falha na publicação com retry", e);
        }
    }

    /**
     * Determina o tópico baseado no tipo de evento
     */
    private String getTopicForEvent(DomainEvent event) {
        String eventType = event.getClass().getSimpleName();

        return switch (eventType) {
            case "OrderCreatedEvent", "OrderConfirmedEvent", "OrderCancelledEvent",
                 "OrderShippedEvent", "OrderDeliveredEvent" -> ORDER_EVENTS_TOPIC;
            case "PaymentRequestedEvent", "PaymentProcessedEvent",
                 "PaymentFailedEvent" -> PAYMENT_EVENTS_TOPIC;
            default -> ORDER_EVENTS_TOPIC; // Default para eventos de pedido
        };
    }

    /**
     * Gera chave da mensagem para particionamento
     * A chave garante que eventos do mesmo agregado
     * vão para a mesma partição, mantendo ordem
     */
    private String generateMessageKey(DomainEvent event) {
        return event.aggregateId().toString();
    }

    /**
     * Callback para publicação bem-sucedida
     */
    private void handleSuccessfulPublish(DomainEvent event, SendResult<String, Object> result) {
        var metadata = result.getRecordMetadata();

        logger.info("Evento {} publicado com sucesso: topic={}, partition={}, offset={}",
                event.getClass().getSimpleName(),
                metadata.topic(),
                metadata.partition(),
                metadata.offset());

        // Aqui poderia ser adicionado:
        // - Métricas de eventos publicados
        // - Log de auditoria
        // - Cache de eventos processados (idempotência)
    }

    /**
     * Callback para falha na publicação
     */
    private void handleFailedPublish(DomainEvent event, Throwable throwable) {
        logger.error("Falha ao publicar evento {}: {}",
                event.getClass().getSimpleName(),
                throwable.getMessage(),
                throwable);

        // Aqui poderia ser adicionado:
        // - Dead Letter Queue
        // - Alertas para ops
        // - Métricas de erro
        // - Retry policy customizada
    }
}