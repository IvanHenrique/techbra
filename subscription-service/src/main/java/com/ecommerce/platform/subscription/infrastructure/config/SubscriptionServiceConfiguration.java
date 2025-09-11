package com.ecommerce.platform.subscription.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

import java.time.Duration;
import java.util.List;

/**
 * Configuração Principal do Subscription Service
 * Centraliza todas as configurações do Spring necessárias para
 * implementar a arquitetura hexagonal com foco em assinaturas recorrentes.
 * Configurações incluídas:
 * - JPA/Hibernate para persistência
 * - Kafka para mensageria
 * - Cache Redis para performance
 * - Quartz Scheduler para cobrança recorrente
 * - OpenAPI para documentação
 * - Transações e retry mechanism
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.ecommerce.platform.subscription.adapter.out.database")
@EntityScan(basePackages = {
    "com.ecommerce.platform.subscription.domain.model",
    "com.ecommerce.platform.shared.domain",
    "com.ecommerce.platform.subscription.infrastructure.persistence"
})
@EnableTransactionManagement
@EnableKafka
@EnableCaching
@EnableRetry
@EnableScheduling
public class SubscriptionServiceConfiguration {

    /**
     * Configuração da documentação OpenAPI
     * Define metadados da API para geração automática da documentação
     * Swagger UI disponível em: /swagger-ui.html
     */
    @Bean
    public OpenAPI subscriptionServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Subscription Service API")
                .description("API para gestão de assinaturas recorrentes com cobrança automática")
                .version("1.0.0")
                .contact(new Contact()
                    .name("E-commerce Platform Team")
                    .email("dev@ecommerce-platform.com"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8081/subscription-service")
                    .description("Ambiente de desenvolvimento"),
                new Server()
                    .url("https://api.ecommerce-platform.com/subscription-service")
                    .description("Ambiente de produção")
            ));
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();
    }
}