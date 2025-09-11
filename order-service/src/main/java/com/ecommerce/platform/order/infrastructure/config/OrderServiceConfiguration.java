package com.ecommerce.platform.order.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

/**
 * Configuração Principal do Order Service
 * Centraliza todas as configurações do Spring necessárias para
 * implementar a arquitetura hexagonal com as tecnologias escolhidas.
 * Configurações incluídas:
 * - JPA/Hibernate para persistência
 * - Kafka para mensageria
 * - Cache Redis
 * - OpenAPI para documentação
 * - Transações
 * - Retry mechanism
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.ecommerce.platform.order.adapter.out.database")
@EntityScan(basePackages = {
    "com.ecommerce.platform.order.domain.model",
    "com.ecommerce.platform.shared.domain",
    "com.ecommerce.platform.order.infrastructure.persistence" // Inclui os AttributeConverters
})
@EnableTransactionManagement
@EnableKafka
@EnableCaching
@EnableRetry
public class OrderServiceConfiguration {

    /**
     * Configuração da documentação OpenAPI
     * Define metadados da API para geração automática da documentação
     * Swagger UI disponível em: /swagger-ui.html
     */
    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Order Service API")
                .description("API para gestão de pedidos no e-commerce com arquitetura hexagonal")
                .version("1.0.0")
                .contact(new Contact()
                    .name("E-commerce Platform Team")
                    .email("dev@ecommerce-platform.com"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080/order-service")
                    .description("Ambiente de desenvolvimento"),
                new Server()
                    .url("https://api.ecommerce-platform.com/order-service")
                    .description("Ambiente de produção")
            ));
    }
}