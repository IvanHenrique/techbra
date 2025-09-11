package com.ecommerce.platform.shared.infrastructure.kafka;

import com.ecommerce.platform.shared.domain.DomainEvent;
import com.ecommerce.platform.shared.domain.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementação do Event Publisher usando Apache Kafka
 * Esta implementação garante:
 * - **Delivery**: Entrega garantida com confirmação do broker
 * - **Ordering**: Ordem preservada por partição via partition key
 * - **Durability**: Persistência nos tópicos Kafka
 * - **Scalability**: Paralelismo via múltiplas partições
 * - **Observability**: Métricas e logs detalhados
 * Estratégia de Roteamento:
 * - Eventos de Order → "order.events" 
 * - Eventos de Subscription → "subscription.events"
 * - Eventos de Payment → "payment.events"
 * - Eventos de Billing → "billing.events"
 * - Outros → tópico baseado no nome da classe
 * Padrões implementados:
 * - Event Sourcing: Todos os eventos são persistidos
 * - At-least-once delivery: Garantia de entrega
 * - Idempotency: Eventos podem ser reprocessados safely
 */
@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Métricas internas para observabilidade
    private final AtomicLong publishedEvents = new AtomicLong(0);
    private final AtomicLong failedEvents = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);

    @Autowired
    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        logger.info("KafkaEventPublisher initialized with bootstrap servers: {}", 
                   kafkaTemplate.getProducerFactory().getConfigurationProperties().get("bootstrap.servers"));
    }

    /**
     * Publica evento usando roteamento automático baseado no tipo
     * O tópico é determinado automaticamente baseado no tipo do evento:
     * - OrderCreated/OrderConfirmed/OrderCancelled → "order.events"
     * - SubscriptionCreated/SubscriptionActivated → "subscription.events" 
     * - PaymentRequested/PaymentProcessed → "payment.events"
     * - BillingScheduled/BillingProcessed → "billing.events"
     */
    @Override
    public CompletableFuture<Void> publish(DomainEvent event) {
        String topic = resolveTopicFromEvent(event);
        String partitionKey = event.getAggregateId();
        
        return publish(topic, event, partitionKey);
    }

    /**
     * Publica evento em tópico específico
     */
    @Override
    public CompletableFuture<Void> publish(String topic, DomainEvent event) {
        String partitionKey = event.getAggregateId();
        return publish(topic, event, partitionKey);
    }

    /**
     * Publica evento com partition key específica
     */
    @Override
    public CompletableFuture<Void> publish(DomainEvent event, String partitionKey) {
        String topic = resolveTopicFromEvent(event);
        return publish(topic, event, partitionKey);
    }

    /**
     * Implementação interna de publicação com todas as configurações
     */
    private CompletableFuture<Void> publish(String topic, DomainEvent event, String partitionKey) {
        long startTime = System.currentTimeMillis();
        
        logger.debug("Publishing event {} to topic {} with partition key {}", 
                    event.getEventType(), topic, partitionKey);

        return kafkaTemplate.send(topic, partitionKey, event)
            .thenApply(this::handleSuccess)
            .handle((result, throwable) -> {
                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;
                totalLatency.addAndGet(latency);
                
                if (throwable != null) {
                    return handleFailure(event, topic, throwable, latency);
                } else {
                    return handleSuccessWithMetrics(event, topic, latency);
                }
            });
    }

    /**
     * Publica múltiplos eventos em batch
     * Estratégia:
     * - Agrupa eventos por tópico para otimizar envio
     * - Processa todos assincronamente
     * - Falha todo o batch se qualquer evento falhar
     */
    @Override
    public CompletableFuture<Void> publishBatch(List<DomainEvent> events) {
        logger.debug("Publishing batch of {} events", events.size());
        
        if (events.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Converte todos os eventos para CompletableFuture
        List<CompletableFuture<Void>> futures = events.stream()
            .map(this::publish)
            .toList();
        
        // Aguarda todos completarem (falha se qualquer um falhar)
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> logger.info("Successfully published batch of {} events", events.size()));
    }

    /**
     * Verifica saúde do publisher tentando obter metadados
     */
    @Override
    public boolean isHealthy() {
        try {
            // Tenta obter metadados para verificar conectividade
            var producer = kafkaTemplate.getProducerFactory().createProducer();
            var metadata = producer.partitionsFor("order.events");
            producer.close();
            return metadata != null && !metadata.isEmpty();
        } catch (Exception e) {
            logger.warn("Health check failed for KafkaEventPublisher", e);
            return false;
        }
    }

    /**
     * Retorna métricas de publicação para observabilidade
     */
    @Override
    public Map<String, Object> getMetrics() {
        long published = publishedEvents.get();
        long failed = failedEvents.get();
        long avgLatency = published > 0 ? totalLatency.get() / published : 0;
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("events.published.total", published);
        metrics.put("events.failed.total", failed);
        metrics.put("events.success.rate", published > 0 ? (double) (published - failed) / published : 0.0);
        metrics.put("events.latency.avg.ms", avgLatency);
        metrics.put("publisher.healthy", isHealthy());
        
        return metrics;
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Resolve o tópico baseado no tipo do evento
     * Convenção de roteamento:
     * - Eventos que começam com "ORDER" → "order.events"
     * - Eventos que começam com "SUBSCRIPTION" → "subscription.events"
     * - Eventos que começam com "PAYMENT" → "payment.events"
     * - Eventos que começam com "BILLING" → "billing.events"
     * - Eventos que começam com "NOTIFICATION" → "notification.events"
     * - Outros → tópico baseado no contexto do evento
     */
    private String resolveTopicFromEvent(DomainEvent event) {
        String eventType = event.getEventType();
        
        if (eventType.startsWith("ORDER")) {
            return "order.events";
        } else if (eventType.startsWith("SUBSCRIPTION")) {
            return "subscription.events";
        } else if (eventType.startsWith("PAYMENT")) {
            return "payment.events";
        } else if (eventType.startsWith("BILLING")) {
            return "billing.events";
        } else if (eventType.startsWith("NOTIFICATION")) {
            return "notification.events";
        } else {
            // Fallback: usa contexto do evento para criar nome do tópico
            String context = event.getContext().toLowerCase();
            return context + ".events";
        }
    }

    /**
     * Manipula sucesso da publicação (callback interno do Kafka)
     */
    private Void handleSuccess(SendResult<String, Object> result) {
        var metadata = result.getRecordMetadata();
        logger.debug("Event published successfully to topic {} partition {} offset {}", 
                    metadata.topic(), metadata.partition(), metadata.offset());
        return null;
    }

    /**
     * Manipula falha na publicação
     */
    private Void handleFailure(DomainEvent event, String topic, Throwable throwable, long latency) {
        failedEvents.incrementAndGet();
        
        logger.error("Failed to publish event {} to topic {} after {}ms", 
                    event.getEventType(), topic, latency, throwable);
        
        // Re-lança a exceção para que o CompletableFuture falhe
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else {
            throw new EventPublicationException("Failed to publish event", throwable);
        }
    }

    /**
     * Manipula sucesso com atualização de métricas
     */
    private Void handleSuccessWithMetrics(DomainEvent event, String topic, long latency) {
        publishedEvents.incrementAndGet();
        
        logger.info("Successfully published event {} to topic {} in {}ms", 
                   event.getEventType(), topic, latency);
        
        return null;
    }

    /**
     * Exception customizada para falhas de publicação
     */
    public static class EventPublicationException extends RuntimeException {
        public EventPublicationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}