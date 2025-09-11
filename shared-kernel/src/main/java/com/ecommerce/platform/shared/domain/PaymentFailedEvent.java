package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando um pagamento falha
 * Este evento marca a falha no processamento de uma transação e
 * desencadeia ações de compensação nos demais serviços:
 * Fluxo de processamento:
 * 1. Payment Service: Falha no pagamento → PaymentFailed
 * 2. Order Service: Cancela pedido ou marca como aguardando pagamento
 * 3. Subscription Service: Marca assinatura como inadimplente
 * 4. Inventory Service: Libera reserva de produtos
 * 5. Notification Service: Notifica cliente sobre falha
 * 6. Billing Service: Agenda nova tentativa (se aplicável)
 * Consumers deste evento:
 * - Order Service: Para cancelar ou suspender pedidos
 * - Subscription Service: Para marcar inadimplência
 * - Inventory Service: Para liberar reservas
 * - Notification Service: Para notificar cliente
 * - Billing Service: Para agendar retry
 * - Analytics Service: Para métricas de conversão
 * Padrões implementados:
 * - Event Sourcing: Captura falha de pagamento
 * - Saga Pattern: Executa compensating actions
 * - Circuit Breaker: Pode disparar proteções contra fraude
 */
public record PaymentFailedEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("occurredAt") LocalDateTime occurredAt,
        @JsonProperty("aggregateId") String aggregateId,
        @JsonProperty("paymentId") String paymentId,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("subscriptionId") String subscriptionId,
        @JsonProperty("customerId") String customerId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("paymentMethod") String paymentMethod,
        @JsonProperty("failureReason") String failureReason,
        @JsonProperty("failureCode") String failureCode,
        @JsonProperty("gatewayResponse") String gatewayResponse,
        @JsonProperty("retryAttempt") Integer retryAttempt,
        @JsonProperty("canRetry") Boolean canRetry,
        @JsonProperty("nextRetryDate") LocalDateTime nextRetryDate
) implements DomainEvent {

    /**
     * Constructor para criação via Jackson (deserialização JSON)
     */
    @JsonCreator
    public PaymentFailedEvent {
        // Validações para garantir integridade do evento
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at cannot be null");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or blank");
        }
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("Payment ID cannot be null or blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("Failure reason cannot be null or blank");
        }
    }

    // ===== IMPLEMENTAÇÃO DOS MÉTODOS DA INTERFACE DomainEvent =====

    /**
     * Implementação explícita do método getEventId() da interface
     */
    @Override
    public UUID getEventId() {
        return eventId;
    }

    /**
     * Implementação explícita do método getOccurredAt() da interface
     */
    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    /**
     * Implementação explícita do método getAggregateId() da interface
     */
    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    /**
     * Implementação explícita do método getEventType() da interface
     */
    @Override
    public String getEventType() {
        return "PAYMENT_FAILED";
    }

    // ===== MÉTODOS DE NEGÓCIO ESPECÍFICOS =====

    /**
     * Factory method para falhas de pagamento de pedidos
     */
    public static PaymentFailedEvent forOrder(
            String paymentId,
            String orderId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String failureReason,
            String failureCode,
            String gatewayResponse,
            Integer retryAttempt,
            Boolean canRetry,
            LocalDateTime nextRetryDate) {

        return new PaymentFailedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                paymentId, // aggregateId é o paymentId
                paymentId,
                orderId,
                null, // subscriptionId é null para pedidos únicos
                customerId,
                amount,
                currency,
                paymentMethod,
                failureReason,
                failureCode,
                gatewayResponse,
                retryAttempt != null ? retryAttempt : 1,
                canRetry != null ? canRetry : false,
                nextRetryDate
        );
    }

    /**
     * Factory method para falhas de pagamento de assinaturas
     */
    public static PaymentFailedEvent forSubscription(
            String paymentId,
            String subscriptionId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String failureReason,
            String failureCode,
            String gatewayResponse,
            Integer retryAttempt,
            Boolean canRetry,
            LocalDateTime nextRetryDate) {

        return new PaymentFailedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                paymentId, // aggregateId é o paymentId
                paymentId,
                null, // orderId é null para assinaturas
                subscriptionId,
                customerId,
                amount,
                currency,
                paymentMethod,
                failureReason,
                failureCode,
                gatewayResponse,
                retryAttempt != null ? retryAttempt : 1,
                canRetry != null ? canRetry : false,
                nextRetryDate
        );
    }

    /**
     * Verifica se é uma falha de pagamento de pedido único
     */
    public boolean isOrderPayment() {
        return orderId != null && subscriptionId == null;
    }

    /**
     * Verifica se é uma falha de pagamento de assinatura
     */
    public boolean isSubscriptionPayment() {
        return subscriptionId != null && orderId == null;
    }

    /**
     * Verifica se é possível tentar novamente
     */
    public boolean isRetryable() {
        return Boolean.TRUE.equals(canRetry) && nextRetryDate != null;
    }

    /**
     * Verifica se já esgotou as tentativas
     */
    public boolean hasExhaustedRetries() {
        return !isRetryable() || (retryAttempt != null && retryAttempt >= 3);
    }

    /**
     * Verifica se é primeira tentativa
     */
    public boolean isFirstAttempt() {
        return retryAttempt == null || retryAttempt <= 1;
    }

    /**
     * Classifica o tipo de falha baseado no código
     */
    public FailureType getFailureType() {
        if (failureCode == null) {
            return FailureType.UNKNOWN;
        }

        return switch (failureCode.toUpperCase()) {
            case "INSUFFICIENT_FUNDS", "INSUFFICIENT_BALANCE" -> FailureType.INSUFFICIENT_FUNDS;
            case "INVALID_CARD", "EXPIRED_CARD", "BLOCKED_CARD" -> FailureType.CARD_ISSUE;
            case "SECURITY_VIOLATION", "FRAUD_DETECTED" -> FailureType.SECURITY;
            case "NETWORK_ERROR", "TIMEOUT", "GATEWAY_ERROR" -> FailureType.TECHNICAL;
            case "INVALID_CVV", "INVALID_PIN" -> FailureType.AUTHENTICATION;
            case "TRANSACTION_LIMIT_EXCEEDED" -> FailureType.LIMIT_EXCEEDED;
            default -> FailureType.UNKNOWN;
        };
    }

    /**
     * Verifica se deve cancelar definitivamente (não retryable)
     */
    public boolean shouldCancelDefinitively() {
        FailureType type = getFailureType();
        return type == FailureType.SECURITY ||
                type == FailureType.CARD_ISSUE ||
                hasExhaustedRetries();
    }

    /**
     * Verifica se deve notificar o cliente imediatamente
     */
    public boolean shouldNotifyCustomer() {
        // Notifica sempre na primeira tentativa ou quando esgota retries
        return isFirstAttempt() || hasExhaustedRetries();
    }

    /**
     * Verifica se o pagamento foi processado via cartão de crédito
     */
    public boolean isCreditCardPayment() {
        return "CREDIT_CARD".equals(paymentMethod);
    }

    /**
     * Verifica se o pagamento foi processado via PIX
     */
    public boolean isPixPayment() {
        return "PIX".equals(paymentMethod);
    }

    /**
     * Verifica se o pagamento foi processado via boleto
     */
    public boolean isBoletoPayment() {
        return "BOLETO".equals(paymentMethod);
    }

    /**
     * Enum para classificação de tipos de falha
     */
    public enum FailureType {
        INSUFFICIENT_FUNDS,    // Saldo insuficiente
        CARD_ISSUE,           // Problema com cartão
        SECURITY,             // Violação de segurança/fraude
        TECHNICAL,            // Erro técnico/rede
        AUTHENTICATION,       // Erro de autenticação
        LIMIT_EXCEEDED,       // Limite excedido
        UNKNOWN               // Tipo desconhecido
    }
}