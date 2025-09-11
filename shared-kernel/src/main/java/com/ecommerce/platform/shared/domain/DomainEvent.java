package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface base para todos os Domain Events
 * Implementa padrões fundamentais do Event-Driven Architecture:
 * 1. **Event Sourcing**: Cada evento representa uma mudança de estado no domínio
 * 2. **Immutability**: Eventos são imutáveis por natureza
 * 3. **Traceability**: Cada evento tem ID único e timestamp para auditoria
 * 4. **Polymorphism**: Suporte a diferentes tipos de eventos via Jackson
 * Características importantes:
 * - ID único para idempotência e rastreamento
 * - Timestamp para ordenação temporal
 * - Aggregate ID para correlação com entidades
 * - Event Type para roteamento e processamento
 * Utilizado por:
 * - Event Publishers para publicação
 * - Event Handlers para consumo
 * - Event Store para persistência (se implementado)
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "ORDER_CREATED"),
        @JsonSubTypes.Type(value = OrderConfirmedEvent.class, name = "ORDER_CONFIRMED"),
        @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "ORDER_CANCELLED"),
        @JsonSubTypes.Type(value = SubscriptionCreatedEvent.class, name = "SUBSCRIPTION_CREATED"),
        @JsonSubTypes.Type(value = SubscriptionActivatedEvent.class, name = "SUBSCRIPTION_ACTIVATED"),
        @JsonSubTypes.Type(value = SubscriptionCancelledEvent.class, name = "SUBSCRIPTION_CANCELLED"),
        @JsonSubTypes.Type(value = PaymentRequestedEvent.class, name = "PAYMENT_REQUESTED"),
        @JsonSubTypes.Type(value = PaymentProcessedEvent.class, name = "PAYMENT_PROCESSED"),
        @JsonSubTypes.Type(value = PaymentFailedEvent.class, name = "PAYMENT_FAILED"),
        @JsonSubTypes.Type(value = BillingScheduledEvent.class, name = "BILLING_SCHEDULED"),
        @JsonSubTypes.Type(value = BillingProcessedEvent.class, name = "BILLING_PROCESSED"),
        @JsonSubTypes.Type(value = BillingFailedEvent.class, name = "BILLING_FAILED")
})
public interface DomainEvent {

    /**
     * Identificador único do evento
     * Utilizado para:
     * - Garantir idempotência no processamento
     * - Rastreamento em logs e auditoria
     * - Correlação entre eventos relacionados
     *
     * @return UUID único do evento
     */
    UUID getEventId();

    /**
     * Timestamp de quando o evento ocorreu
     * Utilizado para:
     * - Ordenação temporal dos eventos
     * - Implementação de Event Sourcing
     * - Auditoria e troubleshooting
     * - TTL (Time To Live) de eventos
     *
     * @return Data e hora da ocorrência do evento
     */
    LocalDateTime getOccurredAt();

    /**
     * Identificador do agregado que gerou o evento
     * Utilizado para:
     * - Correlacionar eventos com entidades de domínio
     * - Particionamento em tópicos Kafka
     * - Reconstrução de estado via Event Sourcing
     * - Filtros e consultas por entidade
     *
     * @return ID do agregado que originou o evento
     */
    String getAggregateId();

    /**
     * Tipo do evento para roteamento e processamento
     * Utilizado para:
     * - Roteamento para handlers específicos
     * - Filtros em consumers Kafka
     * - Versionamento de eventos
     * - Métricas por tipo de evento
     *
     * @return Tipo do evento (ex: ORDER_CREATED, PAYMENT_PROCESSED)
     */
    String getEventType();

    /**
     * Versão do schema do evento
     * Utilizado para:
     * - Evolução de schemas sem quebrar compatibilidade
     * - Migração de eventos antigos
     * - Validação de formato
     *
     * @return Versão do schema (ex: "1.0", "2.1")
     */
    default String getSchemaVersion() {
        return "1.0";
    }

    /**
     * Contexto de origem do evento
     * Utilizado para:
     * - Rastreamento de origem (qual serviço gerou)
     * - Debugging distribuído
     * - Auditoria de sistemas
     *
     * @return Nome do contexto/serviço que gerou o evento
     */
    default String getContext() {
        return getClass().getPackageName().split("\\.")[3]; // Extrai nome do serviço do package
    }

    /**
     * Metadados adicionais do evento
     * Pode conter:
     * - Correlation ID para rastreamento distribuído
     * - User ID do usuário que iniciou a ação
     * - Source IP da requisição
     * - Additional context para debugging
     *
     * @return Map com metadados opcionais
     */
    default java.util.Map<String, Object> getMetadata() {
        return java.util.Map.of(
                "context", getContext(),
                "schemaVersion", getSchemaVersion(),
                "eventClass", getClass().getSimpleName()
        );
    }
}