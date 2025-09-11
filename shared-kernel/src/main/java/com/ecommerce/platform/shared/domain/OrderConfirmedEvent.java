package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando um pedido é confirmado
 * Este evento marca a confirmação de um pedido após o pagamento
 * ser processado com sucesso e desencadeia o processo de fulfillment.
 * Fluxo de processamento:
 * 1. Order Service: Confirma pedido → OrderConfirmed
 * 2. Inventory Service: Processa separação dos itens
 * 3. Shipping Service: Prepara envio
 * 4. Notification Service: Notifica cliente sobre confirmação
 * Consumers deste evento:
 * - Inventory Service: Para separar produtos
 * - Shipping Service: Para preparar envio
 * - Notification Service: Para notificar confirmação
 * - Analytics Service: Para métricas de conversão
 * Padrões implementados:
 * - Event Sourcing: Captura confirmação do pedido
 * - Saga Pattern: Continua fluxo de fulfillment
 * - CQRS: Atualiza modelos de leitura
 */
public record OrderConfirmedEvent(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("occurredAt") LocalDateTime occurredAt,
    @JsonProperty("aggregateId") String aggregateId,
    @JsonProperty("orderId") String orderId,
    @JsonProperty("customerId") String customerId,
    @JsonProperty("orderTotal") BigDecimal orderTotal,
    @JsonProperty("currency") String currency,
    @JsonProperty("paymentId") String paymentId,
    @JsonProperty("confirmedAt") LocalDateTime confirmedAt,
    @JsonProperty("orderType") String orderType
) implements DomainEvent {

    @JsonCreator
    public OrderConfirmedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at cannot be null");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or blank");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order total must be positive");
        }
        if (confirmedAt == null) {
            throw new IllegalArgumentException("Confirmed at cannot be null");
        }
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    @Override
    public String getEventType() {
        return "ORDER_CONFIRMED";
    }

    public static OrderConfirmedEvent create(
            String orderId,
            String customerId,
            BigDecimal orderTotal,
            String currency,
            String paymentId,
            String orderType) {
        
        return new OrderConfirmedEvent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            orderId,
            orderId,
            customerId,
            orderTotal,
            currency,
            paymentId,
            LocalDateTime.now(),
            orderType
        );
    }

    public boolean isSubscriptionOrder() {
        return "SUBSCRIPTION_GENERATED".equals(orderType);
    }

    public boolean isOneTimeOrder() {
        return "ONE_TIME".equals(orderType);
    }
}