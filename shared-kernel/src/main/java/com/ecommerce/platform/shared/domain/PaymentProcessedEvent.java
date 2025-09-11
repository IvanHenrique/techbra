package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando um pagamento é processado com sucesso
 * Este evento marca a confirmação financeira de uma transação e
 * desencadeia ações nos demais serviços:
 * Fluxo de processamento:
 * 1. Payment Service: Processa pagamento → PaymentProcessed
 * 2. Order Service: Confirma pedido e inicia fulfillment
 * 3. Subscription Service: Ativa assinatura (se aplicável)
 * 4. Inventory Service: Confirma reserva de produtos
 * 5. Notification Service: Notifica cliente sobre confirmação
 * Consumers deste evento:
 * - Order Service: Para confirmar pedidos
 * - Subscription Service: Para ativar assinaturas
 * - Inventory Service: Para confirmar reservas
 * - Notification Service: Para confirmar ao cliente
 * - Analytics Service: Para métricas financeiras
 * - Billing Service: Para registrar receita
 * Padrões implementados:
 * - Event Sourcing: Captura confirmação de pagamento
 * - Saga Pattern: Continua fluxo de transação distribuída
 * - CQRS: Atualiza modelos de leitura financeiros
 */
public record PaymentProcessedEvent(
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
        @JsonProperty("transactionId") String transactionId,
        @JsonProperty("processedAt") LocalDateTime processedAt,
        @JsonProperty("gatewayResponse") String gatewayResponse,
        @JsonProperty("fees") BigDecimal fees
) implements DomainEvent {

    /**
     * Constructor para criação via Jackson (deserialização JSON)
     */
    @JsonCreator
    public PaymentProcessedEvent {
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
        if (processedAt == null) {
            throw new IllegalArgumentException("Processed at cannot be null");
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
        return "PAYMENT_PROCESSED";
    }

    // ===== MÉTODOS DE NEGÓCIO ESPECÍFICOS =====

    /**
     * Factory method para pagamentos de pedidos únicos
     */
    public static PaymentProcessedEvent forOrder(
            String paymentId,
            String orderId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String transactionId,
            String gatewayResponse,
            BigDecimal fees) {

        return new PaymentProcessedEvent(
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
                transactionId,
                LocalDateTime.now(),
                gatewayResponse,
                fees != null ? fees : BigDecimal.ZERO
        );
    }

    /**
     * Factory method para pagamentos de assinaturas
     */
    public static PaymentProcessedEvent forSubscription(
            String paymentId,
            String subscriptionId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String transactionId,
            String gatewayResponse,
            BigDecimal fees) {

        return new PaymentProcessedEvent(
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
                transactionId,
                LocalDateTime.now(),
                gatewayResponse,
                fees != null ? fees : BigDecimal.ZERO
        );
    }

    /**
     * Verifica se é um pagamento de pedido único
     */
    public boolean isOrderPayment() {
        return orderId != null && subscriptionId == null;
    }

    /**
     * Verifica se é um pagamento de assinatura
     */
    public boolean isSubscriptionPayment() {
        return subscriptionId != null && orderId == null;
    }

    /**
     * Calcula o valor líquido (valor - taxas)
     */
    public BigDecimal getNetAmount() {
        return amount.subtract(fees != null ? fees : BigDecimal.ZERO);
    }

    /**
     * Verifica se houve cobrança de taxas
     */
    public boolean hasFees() {
        return fees != null && fees.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calcula o percentual de taxa sobre o valor total
     */
    public BigDecimal getFeePercentage() {
        if (!hasFees()) {
            return BigDecimal.ZERO;
        }
        return fees.divide(amount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
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
     * Verifica se é um pagamento de alto valor (>= R$ 1000)
     */
    public boolean isHighValuePayment() {
        return amount.compareTo(new BigDecimal("1000.00")) >= 0;
    }

    /**
     * Obtém identificador da transação ou do pedido para confirmação
     */
    public String getConfirmationReference() {
        if (isOrderPayment()) {
            return orderId;
        } else if (isSubscriptionPayment()) {
            return subscriptionId;
        }
        return paymentId;
    }

    /**
     * Verifica se precisa de aprovação manual (alto valor + cartão)
     */
    public boolean requiresManualApproval() {
        return isHighValuePayment() && isCreditCardPayment();
    }
}