package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando uma assinatura é criada
 * Este evento inicia o fluxo de processamento de assinaturas e
 * coordena a integração entre vários serviços:
 * Fluxo de processamento:
 * 1. Subscription Service: Cria assinatura → SubscriptionCreated
 * 2. Billing Service: Agenda primeira cobrança
 * 3. Payment Service: Processa primeiro pagamento
 * 4. Order Service: Prepara para pedidos recorrentes
 * 5. Notification Service: Notifica cliente sobre assinatura
 * Consumers deste evento:
 * - Billing Service: Para agendar cobranças recorrentes
 * - Payment Service: Para processar primeiro pagamento
 * - Order Service: Para configurar pedidos automáticos
 * - Notification Service: Para welcome email
 * - Analytics Service: Para métricas de assinaturas
 * Padrões implementados:
 * - Event Sourcing: Captura mudança de estado da assinatura
 * - Saga Pattern: Coordena transação distribuída de ativação
 * - CQRS: Trigger para modelos de leitura
 */
public record SubscriptionCreatedEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("occurredAt") LocalDateTime occurredAt,
        @JsonProperty("aggregateId") String aggregateId,
        @JsonProperty("subscriptionId") String subscriptionId,
        @JsonProperty("customerId") String customerId,
        @JsonProperty("planId") String planId,
        @JsonProperty("planName") String planName,
        @JsonProperty("monthlyPrice") BigDecimal monthlyPrice,
        @JsonProperty("currency") String currency,
        @JsonProperty("billingCycle") String billingCycle,
        @JsonProperty("startDate") LocalDateTime startDate,
        @JsonProperty("nextBillingDate") LocalDateTime nextBillingDate,
        @JsonProperty("trialPeriodDays") Integer trialPeriodDays,
        @JsonProperty("paymentMethodId") String paymentMethodId,
        @JsonProperty("status") String status
) implements DomainEvent {

    /**
     * Constructor para criação via Jackson (deserialização JSON)
     */
    @JsonCreator
    public SubscriptionCreatedEvent {
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
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("Subscription ID cannot be null or blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("Plan ID cannot be null or blank");
        }
        if (monthlyPrice == null || monthlyPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monthly price must be non-negative");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (nextBillingDate == null) {
            throw new IllegalArgumentException("Next billing date cannot be null");
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
        return "SUBSCRIPTION_CREATED";
    }

    // ===== MÉTODOS DE NEGÓCIO ESPECÍFICOS =====

    /**
     * Factory method para criação a partir de dados de domínio
     *
     * @param subscriptionId Identificador da assinatura
     * @param customerId Identificador do cliente
     * @param planId Identificador do plano
     * @param planName Nome do plano para exibição
     * @param monthlyPrice Preço mensal da assinatura
     * @param currency Moeda (BRL, USD, etc)
     * @param billingCycle Ciclo de cobrança (MONTHLY, YEARLY)
     * @param startDate Data de início da assinatura
     * @param nextBillingDate Data da próxima cobrança
     * @param trialPeriodDays Dias de período de teste (null se não houver)
     * @param paymentMethodId Método de pagamento vinculado
     * @return Nova instância do evento
     */
    public static SubscriptionCreatedEvent create(
            String subscriptionId,
            String customerId,
            String planId,
            String planName,
            BigDecimal monthlyPrice,
            String currency,
            String billingCycle,
            LocalDateTime startDate,
            LocalDateTime nextBillingDate,
            Integer trialPeriodDays,
            String paymentMethodId) {

        return new SubscriptionCreatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                subscriptionId, // aggregateId é o próprio subscriptionId
                subscriptionId,
                customerId,
                planId,
                planName,
                monthlyPrice,
                currency,
                billingCycle,
                startDate,
                nextBillingDate,
                trialPeriodDays,
                paymentMethodId,
                "PENDING" // Status inicial sempre PENDING
        );
    }

    /**
     * Verifica se a assinatura está em período de teste
     */
    public boolean isInTrialPeriod() {
        return trialPeriodDays != null && trialPeriodDays > 0;
    }

    /**
     * Verifica se é uma assinatura mensal
     */
    public boolean isMonthlyBilling() {
        return "MONTHLY".equals(billingCycle);
    }

    /**
     * Verifica se é uma assinatura anual
     */
    public boolean isYearlyBilling() {
        return "YEARLY".equals(billingCycle);
    }

    /**
     * Calcula o valor da primeira cobrança
     * Se estiver em período de teste, primeira cobrança pode ser zero
     * ou ter valor promocional
     */
    public BigDecimal getFirstBillingAmount() {
        if (isInTrialPeriod()) {
            // Durante trial, primeira cobrança é zero
            return BigDecimal.ZERO;
        }

        if (isYearlyBilling()) {
            // Cobrança anual = 12 meses
            return monthlyPrice.multiply(BigDecimal.valueOf(12));
        }

        // Cobrança mensal
        return monthlyPrice;
    }

    /**
     * Calcula quando o período de teste termina
     */
    public LocalDateTime getTrialEndDate() {
        if (!isInTrialPeriod()) {
            return startDate;
        }
        return startDate.plusDays(trialPeriodDays);
    }

    /**
     * Verifica se precisa processar cobrança imediatamente
     * True se:
     * - Não está em período de teste
     * - Ou período de teste já terminou
     */
    public boolean shouldBillImmediately() {
        if (!isInTrialPeriod()) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(getTrialEndDate());
    }

    /**
     * Obtém dados para agendamento no billing service
     */
    public BillingScheduleData getBillingScheduleData() {
        return new BillingScheduleData(
                subscriptionId,
                customerId,
                getFirstBillingAmount(),
                currency,
                nextBillingDate,
                billingCycle,
                paymentMethodId
        );
    }

    /**
     * Record para dados de agendamento de cobrança
     * Utilizado pelo Billing Service para agendar cobranças recorrentes
     */
    public record BillingScheduleData(
            @JsonProperty("subscriptionId") String subscriptionId,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("currency") String currency,
            @JsonProperty("scheduledDate") LocalDateTime scheduledDate,
            @JsonProperty("frequency") String frequency,
            @JsonProperty("paymentMethodId") String paymentMethodId
    ) {
        @JsonCreator
        public BillingScheduleData {
            if (subscriptionId == null || subscriptionId.isBlank()) {
                throw new IllegalArgumentException("Subscription ID cannot be null or blank");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount must be non-negative");
            }
            if (scheduledDate == null) {
                throw new IllegalArgumentException("Scheduled date cannot be null");
            }
        }
    }
}