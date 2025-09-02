package com.ecommerce.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Subscription Service Application
 * Microserviço responsável pela gestão de assinaturas e cobrança recorrente.
 * Implementa arquitetura hexagonal com as seguintes responsabilidades:
 * - Criação e gestão de assinaturas
 * - Processamento de cobranças recorrentes
 * - Controle de ciclo de vida (ativa, pausada, cancelada)
 * - Integração com billing para agendamento de cobranças
 * - Geração de pedidos recorrentes
 * Padrões implementados:
 * - Hexagonal Architecture (Ports & Adapters)
 * - Domain-Driven Design
 * - Event-Driven Architecture
 * - Scheduler para tarefas recorrentes (Quartz)
 */
@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.platform.subscription", // Contexto local
    "com.ecommerce.platform.shared"        // Shared kernel
})
@EnableKafka        // Habilita Kafka para mensageria
@EnableCaching      // Habilita cache para performance
@EnableScheduling   // Habilita scheduling para cobranças recorrentes
public class SubscriptionServiceApplication {

    /**
     * Ponto de entrada da aplicação
     * Configurações importantes:
     * - Profile ativo: definido via SPRING_PROFILES_ACTIVE
     * - Porta padrão: 8081 (configurável via application.yml)
     * - Banco de dados: PostgreSQL para produção, H2 para testes
     * - Quartz Scheduler: Para jobs de cobrança recorrente
     */
    public static void main(String[] args) {
        SpringApplication.run(SubscriptionServiceApplication.class, args);
    }
}