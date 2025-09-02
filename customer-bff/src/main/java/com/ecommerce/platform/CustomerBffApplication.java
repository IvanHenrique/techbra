package com.ecommerce.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Customer BFF (Backend for Frontend) Application
 * API Gateway personalizada que agrega dados de múltiplos microserviços
 * para otimizar a experiência dos clients (web, mobile, etc).
 * Responsabilidades principais:
 * - Agregação de dados de múltiplos serviços em uma única chamada
 * - View Models otimizados para diferentes tipos de client
 * - Fallback strategies quando serviços estão indisponíveis
 * - Cache inteligente para reduzir latência
 * - Rate limiting e circuit breakers para resiliência
 * - Transformação de dados para formatos client-friendly
 * Padrões implementados:
 * - Backend for Frontend Pattern
 * - Circuit Breaker (Resilience4j)
 * - Retry with exponential backoff
 * - Bulkhead isolation
 * - Cache-aside pattern
 * - Graceful degradation
 */
@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.platform.bff",       // Contexto local
    "com.ecommerce.platform.shared"     // Shared kernel
})
@EnableCaching  // Habilita cache para agregação eficiente
public class CustomerBffApplication {

    /**
     * Ponto de entrada da aplicação
     * Configurações importantes:
     * - Profile ativo: definido via SPRING_PROFILES_ACTIVE
     * - Porta padrão: 8090 (configurável via application.yml)
     * - Sem banco de dados próprio (stateless)
     * - Integra com todos os outros microserviços
     */
    public static void main(String[] args) {
        SpringApplication.run(CustomerBffApplication.class, args);
    }
}