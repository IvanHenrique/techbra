package com.ecommerce.platform.subscription.domain.port;

import com.ecommerce.platform.shared.domain.DomainEvent;

/**
 * EventPublisher - Port (Interface) do Domínio
 * Define o contrato para publicação de eventos de domínio
 * sem depender de tecnologia específica (Kafka, RabbitMQ, etc).
 * Permite comunicação assíncrona entre bounded contexts
 * seguindo o padrão Event-Driven Architecture.
 * Esta interface é idêntica à do order-service, demonstrando
 * como o shared kernel permite reutilização de contratos.
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