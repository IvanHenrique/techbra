package com.ecommerce.platform.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Order Service Application
 * Microserviço responsável pela gestão de pedidos no e-commerce.
 * Implementa arquitetura hexagonal com as seguintes responsabilidades:
 * - Criação e processamento de pedidos únicos
 * - Gestão de pedidos recorrentes gerados por assinaturas
 * - Integração com serviços de pagamento e estoque
 * - Publicação de eventos para outros bounded contexts
 * Padrões implementados:
 * - Hexagonal Architecture (Ports & Adapters)
 * - Domain-Driven Design
 * - Event-Driven Architecture
 * - CQRS (Command Query Responsibility Segregation)
 */
@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.platform.order",    // Contexto local
    "com.ecommerce.platform.shared"    // Shared kernel
})
@EnableKafka    // Habilita Kafka para mensageria
@EnableCaching  // Habilita cache para performance
public class OrderServiceApplication {

    /**
     * Ponto de entrada da aplicação
     * Configurações importantes:
     * - Profile ativo: definido via SPRING_PROFILES_ACTIVE
     * - Porta padrão: 8080 (configurável via application.yml)
     * - Banco de dados: PostgreSQL para produção, H2 para testes
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}