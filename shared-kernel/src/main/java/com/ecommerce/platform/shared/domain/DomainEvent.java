package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Classe base para todos os eventos de domínio
 * Esta classe implementa o padrão Domain Event, que é fundamental para
 * Event-Driven Architecture e comunicação assíncrona entre bounded contexts.
 * Características importantes:
 * - Imutável (record)
 * - Versionado para evolução
 * - Timestamp para ordenação temporal
 * - Identificador único para idempotência
 * - Agregado de origem para rastreabilidade
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes({
    // Eventos serão registrados aqui conforme implementação
})
public sealed interface DomainEvent permits OrderCreatedEvent, SubscriptionCreatedEvent {
    
    /**
     * Identificador único do evento
     * Usado para garantir idempotência no processamento
     */
    UUID eventId();
    
    /**
     * Identificador do agregado que gerou o evento
     * Permite rastreabilidade e correlação
     */
    UUID aggregateId();
    
    /**
     * Versão do agregado no momento do evento
     * Importante para Event Sourcing e controle de concorrência
     */
    Long aggregateVersion();
    
    /**
     * Timestamp de quando o evento foi gerado
     * Garante ordenação temporal dos eventos
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
    Instant occurredOn();
    
    /**
     * Versão do schema do evento
     * Permite evolução controlada dos eventos
     */
    default String schemaVersion() {
        return "1.0";
    }
    
    /**
     * Contexto limitado de origem
     * Identifica qual bounded context gerou o evento
     */
    String boundedContext();
    
    /**
     * Factory method para criar eventos com metadados padrão
     */
    static <T extends DomainEvent> T create(T event) {
        return event;
    }
}