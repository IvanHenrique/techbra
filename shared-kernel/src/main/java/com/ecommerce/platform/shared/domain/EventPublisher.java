package com.ecommerce.platform.shared.domain;

import com.ecommerce.platform.shared.infrastructure.kafka.KafkaEventPublisher;

import java.util.concurrent.CompletableFuture;

/**
 * Interface para publicação de Domain Events
 * Implementa o padrão Event Publisher do Event-Driven Architecture.
 * Responsável por publicar eventos de domínio para outros bounded contexts.
 * Características implementadas:
 * - **Asynchronous**: Publicação não bloqueia o fluxo principal
 * - **Reliable**: Garante entrega com confirmação
 * - **Decoupled**: Desacopla produtores de consumidores
 * - **Scalable**: Suporta múltiplos listeners e particionamento
 * Padrões suportados:
 * - Event Sourcing: Publica mudanças de estado como eventos
 * - CQRS: Separa commands (escrita) de queries (leitura)
 * - Saga Pattern: Coordena transações distribuídas via eventos
 * Implementações:
 * - KafkaEventPublisher: Para produção com Apache Kafka
 * - InMemoryEventPublisher: Para testes unitários
 * - TestEventPublisher: Para testes de integração
 */
public interface EventPublisher {

    /**
     * Publica um evento de domínio de forma assíncrona
     * O método garante que:
     * - O evento será persistido no tópico apropriado
     * - Será entregue para todos os consumers interessados
     * - Será processado de forma idempotente
     * - Failures serão tratados com retry automático
     * 
     * @param event Evento de domínio a ser publicado
     * @return CompletableFuture que completa quando o evento for persistido
     * @throws KafkaEventPublisher.EventPublicationException se houver falha na publicação
     * Exemplo de uso:
     * ```java
     * SubscriptionCreated event = new SubscriptionCreated(subscription);
     * eventPublisher.publish(event)
     *     .thenRun(() -> log.info("Evento publicado com sucesso"))
     *     .exceptionally(throwable -> {
     *         log.error("Falha ao publicar evento", throwable);
     *         return null;
     *     });
     * ```
     */
    CompletableFuture<Void> publish(DomainEvent event);

    /**
     * Publica um evento com tópico específico (override do roteamento padrão)
     * Utilizado quando:
     * - Precisa publicar em tópico diferente do padrão
     * - Implementa roteamento customizado
     * - Faz broadcast para múltiplos tópicos
     * 
     * @param topic Nome do tópico específico
     * @param event Evento de domínio a ser publicado
     * @return CompletableFuture que completa quando o evento for persistido
     */
    CompletableFuture<Void> publish(String topic, DomainEvent event);

    /**
     * Publica um evento com chave específica para particionamento
     * A chave é utilizada para:
     * - Garantir ordem de eventos do mesmo agregado
     * - Distribuir carga entre partições
     * - Colocar eventos relacionados na mesma partição
     * 
     * @param event Evento de domínio a ser publicado
     * @param partitionKey Chave para particionamento (geralmente aggregateId)
     * @return CompletableFuture que completa quando o evento for persistido
     * Exemplo de uso:
     * ```java
     * // Garante que todos os eventos do mesmo pedido vão para a mesma partição
     * eventPublisher.publish(orderCreated, order.getId().toString());
     * ```
     */
    CompletableFuture<Void> publish(DomainEvent event, String partitionKey);

    /**
     * Publica múltiplos eventos em uma única operação (batch)
     * Vantagens:
     * - Melhor performance para múltiplos eventos
     * - Atomicidade: todos ou nenhum são publicados
     * - Reduz overhead de rede
     * 
     * @param events Lista de eventos a serem publicados
     * @return CompletableFuture que completa quando todos os eventos forem persistidos
     * Exemplo de uso:
     * ```java
     * List<DomainEvent> events = List.of(
     *     new OrderCreated(order),
     *     new PaymentRequested(payment),
     *     new InventoryReserved(inventory)
     * );
     * eventPublisher.publishBatch(events);
     * ```
     */
    CompletableFuture<Void> publishBatch(java.util.List<DomainEvent> events);

    /**
     * Verifica se o publisher está saudável e pronto para publicar
     * Utilizado para:
     * - Health checks da aplicação
     * - Circuit breaker patterns
     * - Graceful shutdown
     * 
     * @return true se o publisher está operacional
     */
    boolean isHealthy();

    /**
     * Obtém métricas de publicação para observabilidade
     * Métricas incluem:
     * - Número de eventos publicados
     * - Taxa de sucesso/erro
     * - Latência média de publicação
     * - Tamanho da fila interna
     * 
     * @return Map com métricas atuais
     */
    java.util.Map<String, Object> getMetrics();
}