package com.ecommerce.platform.order.domain.port;

import com.ecommerce.platform.shared.domain.DomainEvent;

/**
 * EventPublisher - Port (Interface) do Domínio
 * Define o contrato para publicação de eventos de domínio
 * sem depender de tecnologia específica (Kafka, RabbitMQ, etc).
 * Permite comunicação assíncrona entre bounded contexts
 * seguindo o padrão Event-Driven Architecture.
 * Responsabilidades:
 * - Publicar eventos quando estado do domínio muda
 * - Abstrair complexidade da infraestrutura de mensageria
 * - Garantir que outros contextos sejam notificados
 * - Manter logs de auditoria dos eventos publicados
 */
public interface EventPublisher {
    
    /**
     * Publica um evento de domínio
     * 
     * @param event Evento a ser publicado
     */
    void publish(DomainEvent event);
    
    /**
     * Publica múltiplos eventos em uma operação
     * Útil para transações que geram múltiplos eventos
     * 
     * @param events Array de eventos
     */
    void publishAll(DomainEvent... events);
    
    /**
     * Publica evento com retry automático em caso de falha
     * 
     * @param event Evento a ser publicado
     * @param maxRetries Número máximo de tentativas
     */
    void publishWithRetry(DomainEvent event, int maxRetries);
}