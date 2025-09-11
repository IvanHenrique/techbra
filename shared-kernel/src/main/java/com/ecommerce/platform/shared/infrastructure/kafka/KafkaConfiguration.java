package com.ecommerce.platform.shared.infrastructure.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração centralizada do Apache Kafka para Event-Driven Architecture
 * Esta configuração implementa:
 * - Producer Factory para publicação de eventos
 * - Consumer Factory para consumo de eventos
 * - Admin Client para criação de tópicos
 * - Tópicos padronizados para o domínio de e-commerce
 * Padrões implementados:
 * - Event Sourcing: Todos os eventos são persistidos em tópicos
 * - CQRS: Separação entre commands (producers) e queries (consumers)
 * - Idempotência: Configuração para evitar processamento duplicado
 * - Retry Policy: Configuração de tentativas em caso de falha
 */
@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:ecommerce-platform}")
    private String consumerGroupId;

    /**
     * Configuração do Producer Factory
     * Responsável por criar producers que publicam eventos nos tópicos.
     * Configurações importantes:
     * - BOOTSTRAP_SERVERS: Endereços dos brokers Kafka
     * - KEY_SERIALIZER: Serialização das chaves (String)
     * - VALUE_SERIALIZER: Serialização dos valores (JSON)
     * - ACKS: Garantia de entrega (all = aguarda confirmação de todas as réplicas)
     * - RETRIES: Número de tentativas em caso de falha
     * - ENABLE_IDEMPOTENCE: Evita duplicação de mensagens
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Configurações de confiabilidade
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Aguarda confirmação de todas as réplicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // 3 tentativas em caso de falha
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Evita duplicação
        
        // Configurações de performance
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // Tamanho do batch para agrupamento
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5); // Tempo de espera para formar batch
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // Buffer de memória
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Bean do KafkaTemplate
     * Template principal para envio de eventos.
     * Utilizado pelos Event Publishers de cada serviço.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Configuração do Consumer Factory
     * Responsável por criar consumers que consomem eventos dos tópicos.
     * Configurações importantes:
     * - GROUP_ID: Identificação do grupo de consumidores
     * - AUTO_OFFSET_RESET: Estratégia quando não há offset (earliest = desde o início)
     * - KEY_DESERIALIZER: Deserialização das chaves
     * - VALUE_DESERIALIZER: Deserialização dos valores
     * - TRUSTED_PACKAGES: Pacotes confiáveis para deserialização JSON
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Configurações de confiabilidade
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Commit manual para garantir processamento
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10); // Processa até 10 registros por vez
        
        // Configuração para deserialização JSON segura
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ecommerce.platform.*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.ecommerce.platform.shared.domain.DomainEvent");
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Container Factory para Listeners
     * Configura como os listeners de eventos são executados.
     * Configurações importantes:
     * - MANUAL_IMMEDIATE: Commit manual após processamento bem-sucedido
     * - Concurrency: Número de threads para processamento paralelo
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Configuração de commit manual para garantir processamento
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Configuração de concorrência (número de threads por tópico)
        factory.setConcurrency(2);
        
        return factory;
    }

    /**
     * Admin Client para gerenciamento de tópicos
     * Utilizado para criar e gerenciar tópicos automaticamente.
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    // ===== CRIAÇÃO AUTOMÁTICA DE TÓPICOS =====

    /**
     * Tópico para eventos de pedidos
     * Eventos: OrderCreated, OrderConfirmed, OrderCancelled, OrderShipped
     * Partições: 3 (permite paralelismo na escrita/leitura)
     * Réplicas: 1 (adequado para desenvolvimento)
     */
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name("order.events")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 dias de retenção
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Tópico para eventos de assinatura
     * Eventos: SubscriptionCreated, SubscriptionActivated, SubscriptionCancelled
     * Partições: 2 (volume menor que pedidos)
     */
    @Bean
    public NewTopic subscriptionEventsTopic() {
        return TopicBuilder.name("subscription.events")
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 dias de retenção
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Tópico para eventos de pagamento
     * Eventos: PaymentRequested, PaymentProcessed, PaymentFailed
     * Partições: 3 (volume alto, processamento crítico)
     */
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment.events")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 dias de retenção
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Tópico para eventos de cobrança (billing)
     * Eventos: BillingScheduled, BillingProcessed, BillingFailed
     * Partições: 2 (volume moderado, processamento em batch)
     */
    @Bean
    public NewTopic billingEventsTopic() {
        return TopicBuilder.name("billing.events")
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "7776000000") // 90 dias de retenção (compliance)
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Tópico para eventos de notificação
     * Eventos: NotificationRequested, NotificationSent, NotificationFailed
     * Partições: 1 (processamento sequencial para evitar spam)
     */
    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name("notification.events")
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "259200000") // 3 dias de retenção
                .config("cleanup.policy", "delete")
                .build();
    }
}