package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio disparado quando uma assinatura é ativada
 * Este evento marca a transição de uma assinatura do estado PENDING para ACTIVE,
 * geralmente após o primeiro pagamento bem-sucedido, e desencadeia a criação
 * de pedidos recorrentes e configuração de cobranças futuras.
 * Fluxo de processamento:
 * 1. Subscription Service: Ativa assinatura → SubscriptionActivated
 * 2. Order Service: Cria primeiro pedido recorrente
 * 3. Billing Service: Confirma agendamento de cobranças futuras
 * 4. Notification Service: Envia welcome message
 * 5. Analytics Service: Registra conversão de trial para paid
 * Consumers deste evento:
 * - Order Service: Para criar pedidos recorrentes
 * - Billing Service: Para confirmar scheduling
 * - Notification Service: Para welcome flow
 * - Analytics Service: Para métricas de ativação
 * - Customer Service: Para atualizar perfil do cliente
 * Padrões implementados:
 * - Event Sourcing: Captura ativação da assinatura
 * - Saga Pattern: Continua fluxo de onboarding
 * - CQRS: Atualiza read models de assinaturas ativas
 */
public record SubscriptionActivatedEvent(
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
        @JsonProperty("activatedAt") LocalDateTime activatedAt,
        @JsonProperty("nextBillingDate") LocalDateTime nextBillingDate,
        @JsonProperty("firstPaymentId") String firstPaymentId,
        @JsonProperty("wasInTrial") Boolean wasInTrial,
        @JsonProperty("trialEndDate") LocalDateTime trialEndDate,
        @JsonProperty("activationSource") String activationSource
) implements DomainEvent {

    /**
     * Constructor para criação via Jackson (deserialização JSON)
     */
    @JsonCreator
    public SubscriptionActivatedEvent {
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
        if (activatedAt == null) {
            throw new IllegalArgumentException("Activated at cannot be null");
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
        return "SUBSCRIPTION_ACTIVATED";
    }

    // ===== MÉTODOS DE NEGÓCIO ESPECÍFICOS =====

    /**
     * Factory method para ativação após primeiro pagamento
     *
     * @param subscriptionId Identificador da assinatura
     * @param customerId Identificador do cliente
     * @param planId Identificador do plano
     * @param planName Nome do plano para exibição
     * @param monthlyPrice Preço mensal da assinatura
     * @param currency Moeda (BRL, USD, etc)
     * @param billingCycle Ciclo de cobrança (MONTHLY, YEARLY)
     * @param nextBillingDate Data da próxima cobrança
     * @param firstPaymentId ID do pagamento que ativou a assinatura
     * @param wasInTrial Se estava em período de teste
     * @param trialEndDate Data de fim do trial (se aplicável)
     * @return Nova instância do evento
     */
    public static SubscriptionActivatedEvent create(
            String subscriptionId,
            String customerId,
            String planId,
            String planName,
            BigDecimal monthlyPrice,
            String currency,
            String billingCycle,
            LocalDateTime nextBillingDate,
            String firstPaymentId,
            Boolean wasInTrial,
            LocalDateTime trialEndDate) {

        return new SubscriptionActivatedEvent(
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
                LocalDateTime.now(), // activatedAt é agora
                nextBillingDate,
                firstPaymentId,
                wasInTrial != null ? wasInTrial : false,
                trialEndDate,
                "PAYMENT_SUCCESS" // Source padrão é sucesso do pagamento
        );
    }

    /**
     * Factory method para ativação manual (admin/suporte)
     *
     * @param subscriptionId Identificador da assinatura
     * @param customerId Identificador do cliente
     * @param planId Identificador do plano
     * @param planName Nome do plano
     * @param monthlyPrice Preço mensal
     * @param currency Moeda
     * @param billingCycle Ciclo de cobrança
     * @param nextBillingDate Próxima cobrança
     * @param adminUserId ID do usuário admin que ativou
     * @return Nova instância do evento
     */
    public static SubscriptionActivatedEvent createManualActivation(
            String subscriptionId,
            String customerId,
            String planId,
            String planName,
            BigDecimal monthlyPrice,
            String currency,
            String billingCycle,
            LocalDateTime nextBillingDate,
            String adminUserId) {

        return new SubscriptionActivatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                subscriptionId,
                subscriptionId,
                customerId,
                planId,
                planName,
                monthlyPrice,
                currency,
                billingCycle,
                LocalDateTime.now(),
                nextBillingDate,
                null, // Sem payment ID para ativação manual
                false, // Não estava em trial
                null, // Sem trial end date
                "MANUAL_ADMIN:" + adminUserId // Source indica ativação manual
        );
    }

    /**
     * Verifica se a assinatura estava em período de teste
     */
    public boolean wasInTrialPeriod() {
        return Boolean.TRUE.equals(wasInTrial);
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
     * Verifica se foi ativação automática via pagamento
     */
    public boolean isAutomaticActivation() {
        return "PAYMENT_SUCCESS".equals(activationSource) && firstPaymentId != null;
    }

    /**
     * Verifica se foi ativação manual por admin
     */
    public boolean isManualActivation() {
        return activationSource != null && activationSource.startsWith("MANUAL_ADMIN:");
    }

    /**
     * Obtém ID do admin que fez ativação manual
     */
    public String getAdminUserId() {
        if (!isManualActivation()) {
            return null;
        }
        return activationSource.substring("MANUAL_ADMIN:".length());
    }

    /**
     * Calcula o valor da próxima cobrança
     */
    public BigDecimal getNextBillingAmount() {
        if (isYearlyBilling()) {
            // Cobrança anual = 12 meses (com possível desconto)
            return monthlyPrice.multiply(BigDecimal.valueOf(12));
        }
        // Cobrança mensal
        return monthlyPrice;
    }

    /**
     * Calcula quantos dias até a próxima cobrança
     */
    public long getDaysUntilNextBilling() {
        return java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now().toLocalDate(),
                nextBillingDate.toLocalDate()
        );
    }

    /**
     * Verifica se a próxima cobrança está próxima (≤ 3 dias)
     */
    public boolean isNextBillingNear() {
        return getDaysUntilNextBilling() <= 3;
    }

    /**
     * Obtém dados para criação de pedido recorrente
     */
    public RecurringOrderData getRecurringOrderData() {
        return new RecurringOrderData(
                subscriptionId,
                customerId,
                planId,
                planName,
                getNextBillingAmount(),
                currency,
                billingCycle,
                nextBillingDate
        );
    }

    /**
     * Obtém dados para notificação de boas-vindas
     */
    public WelcomeNotificationData getWelcomeNotificationData() {
        return new WelcomeNotificationData(
                customerId,
                subscriptionId,
                planName,
                monthlyPrice,
                currency,
                billingCycle,
                nextBillingDate,
                wasInTrialPeriod(),
                activatedAt
        );
    }

    /**
     * Obtém dados para métricas de conversão
     */
    public ConversionMetricsData getConversionMetricsData() {
        return new ConversionMetricsData(
                subscriptionId,
                customerId,
                planId,
                monthlyPrice,
                billingCycle,
                wasInTrialPeriod(),
                trialEndDate,
                activatedAt,
                isAutomaticActivation() ? "PAYMENT" : "MANUAL"
        );
    }

    /**
     * Record para dados de pedido recorrente
     */
    public record RecurringOrderData(
            @JsonProperty("subscriptionId") String subscriptionId,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("planId") String planId,
            @JsonProperty("planName") String planName,
            @JsonProperty("orderAmount") BigDecimal orderAmount,
            @JsonProperty("currency") String currency,
            @JsonProperty("frequency") String frequency,
            @JsonProperty("nextDeliveryDate") LocalDateTime nextDeliveryDate
    ) {
        @JsonCreator
        public RecurringOrderData {
            if (subscriptionId == null || subscriptionId.isBlank()) {
                throw new IllegalArgumentException("Subscription ID cannot be null or blank");
            }
            if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Order amount must be non-negative");
            }
            if (nextDeliveryDate == null) {
                throw new IllegalArgumentException("Next delivery date cannot be null");
            }
        }
    }

    /**
     * Record para dados de notificação de boas-vindas
     */
    public record WelcomeNotificationData(
            @JsonProperty("customerId") String customerId,
            @JsonProperty("subscriptionId") String subscriptionId,
            @JsonProperty("planName") String planName,
            @JsonProperty("monthlyPrice") BigDecimal monthlyPrice,
            @JsonProperty("currency") String currency,
            @JsonProperty("billingCycle") String billingCycle,
            @JsonProperty("nextBillingDate") LocalDateTime nextBillingDate,
            @JsonProperty("wasInTrial") boolean wasInTrial,
            @JsonProperty("activatedAt") LocalDateTime activatedAt
    ) {
        @JsonCreator
        public WelcomeNotificationData {
            if (customerId == null || customerId.isBlank()) {
                throw new IllegalArgumentException("Customer ID cannot be null or blank");
            }
            if (subscriptionId == null || subscriptionId.isBlank()) {
                throw new IllegalArgumentException("Subscription ID cannot be null or blank");
            }
            if (activatedAt == null) {
                throw new IllegalArgumentException("Activated at cannot be null");
            }
        }
    }

    /**
     * Record para dados de métricas de conversão
     */
    public record ConversionMetricsData(
            @JsonProperty("subscriptionId") String subscriptionId,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("planId") String planId,
            @JsonProperty("monthlyRevenue") BigDecimal monthlyRevenue,
            @JsonProperty("billingCycle") String billingCycle,
            @JsonProperty("wasTrialConversion") boolean wasTrialConversion,
            @JsonProperty("trialDuration") LocalDateTime trialEndDate,
            @JsonProperty("conversionDate") LocalDateTime conversionDate,
            @JsonProperty("conversionType") String conversionType
    ) {
        @JsonCreator
        public ConversionMetricsData {
            if (subscriptionId == null || subscriptionId.isBlank()) {
                throw new IllegalArgumentException("Subscription ID cannot be null or blank");
            }
            if (monthlyRevenue == null || monthlyRevenue.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Monthly revenue must be non-negative");
            }
            if (conversionDate == null) {
                throw new IllegalArgumentException("Conversion date cannot be null");
            }
        }
    }
}