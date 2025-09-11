package com.ecommerce.platform.subscription.domain.model;

import com.ecommerce.platform.subscription.domain.enums.SubscriptionStatus;
import com.ecommerce.platform.subscription.domain.enums.BillingCycle;
import com.ecommerce.platform.shared.domain.ValueObjects.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Subscription - Agregado Principal do Domínio de Assinaturas
 * Representa uma assinatura recorrente no sistema de e-commerce.
 * Implementa padrões DDD:
 * - Aggregate Root: Controla o ciclo de vida da assinatura
 * - Rich Domain Model: Lógica de negócio encapsulada
 * - Invariants: Regras de negócio sempre consistentes
 * - Domain Events: Comunica mudanças para outros contextos
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "customer_id")
    private CustomerId customerId;

    @Column(name = "customer_email")
    private CustomerEmail customerEmail;

    @Column(name = "plan_id")
    private String planId; // Referência externa para catálogo de planos

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @NotNull
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle")
    @NotNull
    private BillingCycle billingCycle;

    @Column(name = "monthly_price")
    private BigDecimal monthlyPrice;

    @Column(name = "currency")
    private String currency = "BRL";

    @Column(name = "trial_period_days")
    private Integer trialPeriodDays;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "grace_period_end")
    private LocalDate gracePeriodEnd;

    @Column(name = "payment_method_id")
    private String paymentMethodId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // Construtor protegido para JPA
    protected Subscription() {}

    /**
     * Construtor privado - Use factory methods
     */
    private Subscription(CustomerId customerId, CustomerEmail customerEmail,
                         String planId, BillingCycle billingCycle, Money monthlyPrice,
                         Integer trialPeriodDays, String paymentMethodId) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.planId = planId;
        this.billingCycle = billingCycle;
        this.monthlyPrice = monthlyPrice.amount();
        this.currency = monthlyPrice.currency();
        this.trialPeriodDays = trialPeriodDays;
        this.paymentMethodId = paymentMethodId;
        this.status = SubscriptionStatus.TRIAL;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        // Calcular próxima data de cobrança
        calculateNextBillingDate();
    }

    /**
     * Factory Method: Criar nova assinatura
     */
    public static Subscription create(CustomerId customerId, CustomerEmail customerEmail,
                                      String planId, BillingCycle billingCycle,
                                      Money monthlyPrice, Integer trialPeriodDays,
                                      String paymentMethodId) {
        validateCreationParameters(customerId, customerEmail, planId, monthlyPrice, paymentMethodId);

        return new Subscription(customerId, customerEmail, planId, billingCycle,
                monthlyPrice, trialPeriodDays, paymentMethodId);
    }

    /**
     * Ativa a assinatura após primeiro pagamento
     */
    public void activate() {
        if (status != SubscriptionStatus.TRIAL && status != SubscriptionStatus.PENDING) {
            throw new IllegalStateException("Só é possível ativar assinaturas em trial ou pendentes");
        }

        this.status = SubscriptionStatus.ACTIVE;
        this.activatedAt = Instant.now();
        this.updatedAt = Instant.now();

        // Recalcular próxima cobrança baseada na ativação
        calculateNextBillingDate();
    }

    /**
     * Suspende assinatura por falta de pagamento
     */
    public void suspend() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Só é possível suspender assinaturas ativas");
        }

        this.status = SubscriptionStatus.SUSPENDED;
        this.gracePeriodEnd = LocalDate.now().plusDays(7); // 7 dias de período de graça
        this.updatedAt = Instant.now();
    }

    /**
     * Cancela a assinatura
     */
    public void cancel() {
        if (status == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Assinatura já está cancelada");
        }

        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Reativa assinatura suspensa após pagamento
     */
    public void reactivate() {
        if (status != SubscriptionStatus.SUSPENDED) {
            throw new IllegalStateException("Só é possível reativar assinaturas suspensas");
        }

        this.status = SubscriptionStatus.ACTIVE;
        this.gracePeriodEnd = null;
        this.updatedAt = Instant.now();

        calculateNextBillingDate();
    }

    /**
     * Processa cobrança da assinatura
     */
    public void processBilling() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Só é possível cobrar assinaturas ativas");
        }

        if (nextBillingDate.isAfter(LocalDate.now())) {
            throw new IllegalStateException("Data de cobrança ainda não chegou");
        }

        // Avançar para próxima data de cobrança
        calculateNextBillingDate();
        this.updatedAt = Instant.now();
    }

    /**
     * Calcula próxima data de cobrança baseada no ciclo
     */
    private void calculateNextBillingDate() {
        LocalDate baseDate = (activatedAt != null) ?
                activatedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate() :
                LocalDate.now();

        if (trialPeriodDays != null && trialPeriodDays > 0 && activatedAt == null) {
            // Durante trial, próxima cobrança é após o período de trial
            this.nextBillingDate = baseDate.plusDays(trialPeriodDays);
        } else {
            // Cobrança normal baseada no ciclo
            this.nextBillingDate = switch (billingCycle) {
                case MONTHLY -> baseDate.plusMonths(1);
                case QUARTERLY -> baseDate.plusMonths(3);
                case YEARLY -> baseDate.plusYears(1);
            };
        }
    }

    /**
     * Validações de criação
     */
    private static void validateCreationParameters(CustomerId customerId, CustomerEmail customerEmail,
                                                   String planId, Money monthlyPrice, String paymentMethodId) {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId não pode ser null");
        }
        if (customerEmail == null) {
            throw new IllegalArgumentException("CustomerEmail não pode ser null");
        }
        if (planId == null || planId.trim().isEmpty()) {
            throw new IllegalArgumentException("PlanId não pode ser null ou vazio");
        }
        if (monthlyPrice == null || monthlyPrice.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("MonthlyPrice deve ser maior que zero");
        }
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            throw new IllegalArgumentException("PaymentMethodId não pode ser null ou vazio");
        }
    }

    // Getters
    public UUID getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public CustomerEmail getCustomerEmail() { return customerEmail; }
    public String getPlanId() { return planId; }
    public SubscriptionStatus getStatus() { return status; }
    public BillingCycle getBillingCycle() { return billingCycle; }
    public Money getMonthlyPrice() { return Money.of(monthlyPrice, currency); }
    public Integer getTrialPeriodDays() { return trialPeriodDays; }
    public LocalDate getNextBillingDate() { return nextBillingDate; }
    public LocalDate getGracePeriodEnd() { return gracePeriodEnd; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getCancelledAt() { return cancelledAt; }

    // Métodos de conveniência
    public boolean isActive() { return status == SubscriptionStatus.ACTIVE; }
    public boolean isTrial() { return status == SubscriptionStatus.TRIAL; }
    public boolean isSuspended() { return status == SubscriptionStatus.SUSPENDED; }
    public boolean isCancelled() { return status == SubscriptionStatus.CANCELLED; }
    public boolean isInGracePeriod() {
        return gracePeriodEnd != null && gracePeriodEnd.isAfter(LocalDate.now());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Subscription that = (Subscription) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Subscription{id=%s, customerId=%s, planId=%s, status=%s, monthlyPrice=%s %s}",
                id, customerId, planId, status, monthlyPrice, currency);
    }

    /**
     * Verifica se a assinatura precisa ser cobrada
     */
    public boolean needsBilling() {
        // Só cobra assinaturas ativas
        if (status != SubscriptionStatus.ACTIVE) {
            return false;
        }

        // Verifica se a data de cobrança chegou ou passou
        return nextBillingDate != null &&
                (nextBillingDate.isBefore(LocalDate.now()) || nextBillingDate.isEqual(LocalDate.now()));
    }

    /**
     * Calcula valor da cobrança baseado no ciclo de billing
     */
    public Money calculateBillingAmount() {
        if (monthlyPrice == null) {
            return Money.of(BigDecimal.ZERO, currency);
        }

        return switch (billingCycle) {
            case MONTHLY -> Money.of(monthlyPrice, currency);
            case QUARTERLY -> Money.of(monthlyPrice.multiply(BigDecimal.valueOf(3)), currency);
            case YEARLY -> Money.of(monthlyPrice.multiply(BigDecimal.valueOf(12)), currency);
        };
    }

    /**
     * Processa cobrança bem-sucedida
     */
    public void processSuccessfulBilling() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Só é possível processar cobrança de assinaturas ativas");
        }

        // Avançar para próxima data de cobrança
        calculateNextBillingDate();

        // Limpar período de graça se existir
        this.gracePeriodEnd = null;

        this.updatedAt = Instant.now();
    }

    /**
     * Processa falha de cobrança
     */
    public void processFailedBilling() {
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.SUSPENDED) {
            throw new IllegalStateException("Estado inválido para processar falha de cobrança");
        }

        // Marcar como suspenso
        this.status = SubscriptionStatus.SUSPENDED;

        // Definir período de graça (7 dias)
        this.gracePeriodEnd = LocalDate.now().plusDays(7);

        this.updatedAt = Instant.now();
    }

    /**
     * Getter para Money do preço mensal (compatibilidade)
     */
    public Money getMonthlyAmount() {
        return Money.of(monthlyPrice, currency);
    }
}