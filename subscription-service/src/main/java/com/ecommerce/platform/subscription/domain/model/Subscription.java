package com.ecommerce.platform.subscription.domain.model;

import com.ecommerce.platform.shared.domain.ValueObjects.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Subscription - Agregado Principal do Domínio de Assinaturas
 * Representa uma assinatura recorrente no sistema, controlando:
 * - Ciclo de vida da assinatura (ativa, pausada, cancelada)
 * - Cobrança recorrente automática
 * - Geração de pedidos recorrentes
 * - Gestão de inadimplência
 * Implementa padrões DDD:
 * - Aggregate Root: Controla cobrança e renovações
 * - Rich Domain Model: Lógica de negócio encapsulada
 * - Invariants: Status e datas sempre consistentes
 * - Domain Events: Comunica mudanças para billing service
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "customer_id"))
    private CustomerId customerId;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "customer_email"))
    private CustomerEmail customerEmail;
    
    @Column(name = "plan_id")
    @NotNull
    private String planId; // Referência ao plano de assinatura
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @NotNull
    private SubscriptionStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle")
    @NotNull
    private BillingCycle billingCycle;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "monthly_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money monthlyAmount;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;
    
    @Column(name = "end_date")
    private LocalDate endDate; // Nullable - apenas para assinaturas canceladas
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "grace_period_end")
    private LocalDate gracePeriodEnd; // Para inadimplência
    
    @Column(name = "failed_payment_attempts")
    private int failedPaymentAttempts;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // Construtor protegido para JPA
    protected Subscription() {}
    
    /**
     * Construtor privado - Use factory methods
     */
    private Subscription(CustomerId customerId, CustomerEmail customerEmail, 
                        String planId, BillingCycle billingCycle, Money monthlyAmount) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.planId = planId;
        this.billingCycle = billingCycle;
        this.monthlyAmount = monthlyAmount;
        this.status = SubscriptionStatus.PENDING;
        this.startDate = LocalDate.now();
        this.nextBillingDate = calculateNextBillingDate(this.startDate, billingCycle);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.failedPaymentAttempts = 0;
        this.version = 1L;
    }
    
    /**
     * Factory Method: Criar nova assinatura
     */
    public static Subscription create(CustomerId customerId, CustomerEmail customerEmail,
                                    String planId, BillingCycle billingCycle, Money monthlyAmount) {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId não pode ser null");
        }
        if (customerEmail == null) {
            throw new IllegalArgumentException("CustomerEmail não pode ser null");
        }
        if (planId == null || planId.trim().isEmpty()) {
            throw new IllegalArgumentException("PlanId não pode ser null ou vazio");
        }
        if (billingCycle == null) {
            throw new IllegalArgumentException("BillingCycle não pode ser null");
        }
        if (monthlyAmount == null || monthlyAmount.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("MonthlyAmount deve ser positivo");
        }
        
        return new Subscription(customerId, customerEmail, planId, billingCycle, monthlyAmount);
    }
    
    /**
     * Ativa a assinatura após primeiro pagamento bem-sucedido
     */
    public void activate() {
        if (status != SubscriptionStatus.PENDING) {
            throw new IllegalStateException("Só é possível ativar assinaturas pendentes");
        }
        
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = Instant.now();
        this.failedPaymentAttempts = 0;
        this.gracePeriodEnd = null;
    }
    
    /**
     * Pausa a assinatura temporariamente
     */
    public void pause() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Só é possível pausar assinaturas ativas");
        }
        
        this.status = SubscriptionStatus.PAUSED;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Resume assinatura pausada
     */
    public void resume() {
        if (status != SubscriptionStatus.PAUSED) {
            throw new IllegalStateException("Só é possível resumir assinaturas pausadas");
        }
        
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = Instant.now();
        
        // Recalcular próxima data de cobrança
        this.nextBillingDate = calculateNextBillingDate(LocalDate.now(), billingCycle);
    }
    
    /**
     * Cancela a assinatura
     */
    public void cancel() {
        if (status == SubscriptionStatus.CANCELLED || status == SubscriptionStatus.EXPIRED) {
            throw new IllegalStateException("Assinatura já está inativa");
        }
        
        this.status = SubscriptionStatus.CANCELLED;
        this.endDate = LocalDate.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Processa cobrança recorrente bem-sucedida
     */
    public void processSuccessfulBilling() {
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.PAST_DUE) {
            throw new IllegalStateException("Só é possível cobrar assinaturas ativas ou em atraso");
        }
        
        // Reset de tentativas falhadas
        this.failedPaymentAttempts = 0;
        this.gracePeriodEnd = null;
        
        // Se estava em atraso, reativar
        if (status == SubscriptionStatus.PAST_DUE) {
            this.status = SubscriptionStatus.ACTIVE;
        }
        
        // Calcular próxima data de cobrança
        this.nextBillingDate = calculateNextBillingDate(this.nextBillingDate, billingCycle);
        this.updatedAt = Instant.now();
    }
    
    /**
     * Processa falha na cobrança recorrente
     */
    public void processFailedBilling() {
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.PAST_DUE) {
            throw new IllegalStateException("Só é possível processar falha para assinaturas ativas");
        }
        
        this.failedPaymentAttempts++;
        this.updatedAt = Instant.now();
        
        if (failedPaymentAttempts == 1) {
            // Primeira falha - marcar como em atraso e definir período de graça
            this.status = SubscriptionStatus.PAST_DUE;
            this.gracePeriodEnd = LocalDate.now().plusDays(7); // 7 dias de graça
        } else if (failedPaymentAttempts >= 3) {
            // Terceira falha - cancelar automaticamente
            this.status = SubscriptionStatus.CANCELLED;
            this.endDate = LocalDate.now();
        }
    }
    
    /**
     * Renova assinatura (para assinaturas com prazo determinado)
     */
    public void renew() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Só é possível renovar assinaturas ativas");
        }
        
        this.nextBillingDate = calculateNextBillingDate(this.nextBillingDate, billingCycle);
        this.updatedAt = Instant.now();
    }
    
    /**
     * Verifica se a assinatura precisa ser cobrada
     */
    public boolean needsBilling() {
        return status == SubscriptionStatus.ACTIVE && 
               nextBillingDate != null && 
               !nextBillingDate.isAfter(LocalDate.now());
    }
    
    /**
     * Verifica se está no período de graça
     */
    public boolean isInGracePeriod() {
        return status == SubscriptionStatus.PAST_DUE && 
               gracePeriodEnd != null && 
               !gracePeriodEnd.isBefore(LocalDate.now());
    }
    
    /**
     * Verifica se o período de graça expirou
     */
    public boolean isGracePeriodExpired() {
        return status == SubscriptionStatus.PAST_DUE && 
               gracePeriodEnd != null && 
               gracePeriodEnd.isBefore(LocalDate.now());
    }
    
    /**
     * Calcula valor da próxima cobrança baseado no ciclo
     */
    public Money calculateBillingAmount() {
        return switch (billingCycle) {
            case MONTHLY -> monthlyAmount;
            case QUARTERLY -> monthlyAmount.multiply(3);
            case YEARLY -> monthlyAmount.multiply(12);
        };
    }
    
    /**
     * Calcula próxima data de cobrança
     */
    private LocalDate calculateNextBillingDate(LocalDate fromDate, BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> fromDate.plusMonths(1);
            case QUARTERLY -> fromDate.plusMonths(3);
            case YEARLY -> fromDate.plusYears(1);
        };
    }
    
    /**
     * Atualiza plano da assinatura (upgrade/downgrade)
     */
    public void changePlan(String newPlanId, Money newMonthlyAmount) {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Só é possível alterar plano de assinaturas ativas");
        }
        if (newPlanId == null || newPlanId.trim().isEmpty()) {
            throw new IllegalArgumentException("Novo PlanId não pode ser null ou vazio");
        }
        if (newMonthlyAmount == null || newMonthlyAmount.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Novo valor deve ser positivo");
        }
        
        this.planId = newPlanId;
        this.monthlyAmount = newMonthlyAmount;
        this.updatedAt = Instant.now();
    }
    
    // Getters
    public UUID getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public CustomerEmail getCustomerEmail() { return customerEmail; }
    public String getPlanId() { return planId; }
    public SubscriptionStatus getStatus() { return status; }
    public BillingCycle getBillingCycle() { return billingCycle; }
    public Money getMonthlyAmount() { return monthlyAmount; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getNextBillingDate() { return nextBillingDate; }
    public LocalDate getEndDate() { return endDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public LocalDate getGracePeriodEnd() { return gracePeriodEnd; }
    public int getFailedPaymentAttempts() { return failedPaymentAttempts; }
    public Long getVersion() { return version; }
    
    // Métodos de conveniência
    public boolean isActive() { return status == SubscriptionStatus.ACTIVE; }
    public boolean isPending() { return status == SubscriptionStatus.PENDING; }
    public boolean isPaused() { return status == SubscriptionStatus.PAUSED; }
    public boolean isCancelled() { return status == SubscriptionStatus.CANCELLED; }
    public boolean isPastDue() { return status == SubscriptionStatus.PAST_DUE; }
    public boolean isExpired() { return status == SubscriptionStatus.EXPIRED; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Subscription subscription = (Subscription) obj;
        return id.equals(subscription.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("Subscription{id=%s, customerId=%s, planId=%s, status=%s, nextBilling=%s}", 
            id, customerId, planId, status, nextBillingDate);
    }
}