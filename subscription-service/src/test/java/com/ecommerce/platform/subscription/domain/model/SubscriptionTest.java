package com.ecommerce.platform.subscription.domain.model;

import com.ecommerce.platform.shared.domain.ValueObjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes Unitários para Subscription - Agregado Principal
 * Testa todas as regras de negócio e invariantes do domínio de assinaturas.
 * Foca na lógica de cobrança recorrente, ciclo de vida e inadimplência.
 */
class SubscriptionTest {
    
    private CustomerId customerId;
    private CustomerEmail customerEmail;
    private String planId;
    private BillingCycle billingCycle;
    private Money monthlyAmount;
    
    @BeforeEach
    void setUp() {
        customerId = CustomerId.generate();
        customerEmail = CustomerEmail.of("customer@example.com");
        planId = "PREMIUM_PLAN";
        billingCycle = BillingCycle.MONTHLY;
        monthlyAmount = Money.of(BigDecimal.valueOf(29.99), "BRL");
    }
    
    @Nested
    @DisplayName("Criação de Assinaturas")
    class SubscriptionCreationTests {
        
        @Test
        @DisplayName("Deve criar assinatura com dados válidos")
        void shouldCreateSubscriptionWithValidData() {
            // When
            Subscription subscription = Subscription.create(
                customerId, customerEmail, planId, billingCycle, monthlyAmount
            );
            
            // Then
            assertThat(subscription).isNotNull();
            assertThat(subscription.getId()).isNotNull();
            assertThat(subscription.getCustomerId()).isEqualTo(customerId);
            assertThat(subscription.getCustomerEmail()).isEqualTo(customerEmail);
            assertThat(subscription.getPlanId()).isEqualTo(planId);
            assertThat(subscription.getStatus().toString()).isEqualTo("PENDING");
            assertThat(subscription.getBillingCycle()).isEqualTo(billingCycle);
            assertThat(subscription.getMonthlyAmount()).isEqualTo(monthlyAmount);
            assertThat(subscription.getStartDate()).isEqualTo(LocalDate.now());
            assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.now().plusMonths(1));
            assertThat(subscription.getFailedPaymentAttempts()).isEqualTo(0);
            assertThat(subscription.isPending()).isTrue();
        }
        
        @Test
        @DisplayName("Deve falhar ao criar assinatura com dados inválidos")
        void shouldFailToCreateSubscriptionWithInvalidData() {
            // CustomerId null
            assertThatThrownBy(() -> Subscription.create(null, customerEmail, planId, billingCycle, monthlyAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CustomerId não pode ser null");
            
            // CustomerEmail null
            assertThatThrownBy(() -> Subscription.create(customerId, null, planId, billingCycle, monthlyAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CustomerEmail não pode ser null");
            
            // PlanId null
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, null, billingCycle, monthlyAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PlanId não pode ser null");
            
            // PlanId vazio
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, "", billingCycle, monthlyAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PlanId não pode ser null ou vazio");
            
            // BillingCycle null
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, planId, null, monthlyAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BillingCycle não pode ser null");
            
            // MonthlyAmount null
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, planId, billingCycle, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MonthlyAmount deve ser positivo");
            
            // MonthlyAmount zero
            Money zeroAmount = Money.of(BigDecimal.ZERO, "BRL");
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, planId, billingCycle, zeroAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MonthlyAmount deve ser positivo");
        }
        
        @Test
        @DisplayName("Deve calcular próxima data de cobrança corretamente")
        void shouldCalculateNextBillingDateCorrectly() {
            LocalDate today = LocalDate.now();
            
            // Mensal
            Subscription monthly = Subscription.create(customerId, customerEmail, planId, BillingCycle.MONTHLY, monthlyAmount);
            assertThat(monthly.getNextBillingDate()).isEqualTo(today.plusMonths(1));
            
            // Trimestral
            Subscription quarterly = Subscription.create(customerId, customerEmail, planId, BillingCycle.QUARTERLY, monthlyAmount);
            assertThat(quarterly.getNextBillingDate()).isEqualTo(today.plusMonths(3));
            
            // Anual
            Subscription yearly = Subscription.create(customerId, customerEmail, planId, BillingCycle.YEARLY, monthlyAmount);
            assertThat(yearly.getNextBillingDate()).isEqualTo(today.plusYears(1));
        }
    }
    
    @Nested
    @DisplayName("Transições de Status")
    class StatusTransitionTests {
        
        private Subscription subscription;
        
        @BeforeEach
        void setUp() {
            subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount);
        }
        
        @Test
        @DisplayName("Deve ativar assinatura pendente")
        void shouldActivatePendingSubscription() {
            // When
            subscription.activate();
            
            // Then
            assertThat(subscription.isActive()).isTrue();
            assertThat(subscription.getStatus().toString()).isEqualTo("ACTIVE");
            assertThat(subscription.getFailedPaymentAttempts()).isEqualTo(0);
            assertThat(subscription.getGracePeriodEnd()).isNull();
        }
        
        @Test
        @DisplayName("Deve falhar ao ativar assinatura não pendente")
        void shouldFailToActivateNonPendingSubscription() {
            // Given
            subscription.activate(); // Já ativa
            
            // When/Then
            assertThatThrownBy(subscription::activate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Só é possível ativar assinaturas pendentes");
        }
        
        @Test
        @DisplayName("Deve pausar assinatura ativa")
        void shouldPauseActiveSubscription() {
            // Given
            subscription.activate();
            
            // When
            subscription.pause();
            
            // Then
            assertThat(subscription.isPaused()).isTrue();
            assertThat(subscription.getStatus().toString()).isEqualTo("PAUSED");
        }
        
        @Test
        @DisplayName("Deve falhar ao pausar assinatura não ativa")
        void shouldFailToPauseNonActiveSubscription() {
            // When/Then
            assertThatThrownBy(subscription::pause)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Só é possível pausar assinaturas ativas");
        }
        
        @Test
        @DisplayName("Deve resumir assinatura pausada")
        void shouldResumeaPausedSubscription() {
            // Given
            subscription.activate();
            subscription.pause();
            
            // When
            subscription.resume();
            
            // Then
            assertThat(subscription.isActive()).isTrue();
            assertThat(subscription.getStatus().toString()).isEqualTo("ACTIVE");
        }
        
        @Test
        @DisplayName("Deve cancelar assinatura")
        void shouldCancelSubscription() {
            // Given
            subscription.activate();
            
            // When
            subscription.cancel();
            
            // Then
            assertThat(subscription.isCancelled()).isTrue();
            assertThat(subscription.getStatus().toString()).isEqualTo("CANCELLED");
            assertThat(subscription.getEndDate()).isEqualTo(LocalDate.now());
        }
        
        @Test
        @DisplayName("Deve falhar ao cancelar assinatura já inativa")
        void shouldFailToCancelInactiveSubscription() {
            // Given
            subscription.activate();
            subscription.cancel(); // Já cancelada
            
            // When/Then
            assertThatThrownBy(subscription::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Assinatura já está inativa");
        }
    }
    
    @Nested
    @DisplayName("Processamento de Cobrança")
    class BillingProcessingTests {
        
        private Subscription subscription;
        
        @BeforeEach
        void setUp() {
            subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount);
            subscription.activate();
        }
        
        @Test
        @DisplayName("Deve processar cobrança bem-sucedida")
        void shouldProcessSuccessfulBilling() {
            // Given
            LocalDate originalNextBilling = subscription.getNextBillingDate();
            
            // When
            subscription.processSuccessfulBilling();
            
            // Then
            assertThat(subscription.isActive()).isTrue();
            assertThat(subscription.getFailedPaymentAttempts()).isEqualTo(0);
            assertThat(subscription.getGracePeriodEnd()).isNull();
            assertThat(subscription.getNextBillingDate()).isEqualTo(originalNextBilling.plusMonths(1));
        }
        
        @Test
        @DisplayName("Deve reativar assinatura PAST_DUE após cobrança bem-sucedida")
        void shouldReactivatePastDueSubscriptionAfterSuccessfulBilling() {
            // Given
            subscription.processFailedBilling(); // Coloca em PAST_DUE
            assertThat(subscription.isPastDue()).isTrue();
            
            // When
            subscription.processSuccessfulBilling();
            
            // Then
            assertThat(subscription.isActive()).isTrue();
            assertThat(subscription.getFailedPaymentAttempts()).isEqualTo(0);
            assertThat(subscription.getGracePeriodEnd()).isNull();
        }
        
        @Test
        @DisplayName("Deve processar primeira falha de cobrança")
        void shouldProcessFirstBillingFailure() {
            // When
            subscription.processFailedBilling();
            
            // Then
            assertThat(subscription.isPastDue()).isTrue();
            assertThat(subscription.getStatus().toString()).isEqualTo("PAST_DUE");
            assertThat(subscription.getFailedPaymentAttempts()).isEqualTo(1);
            assertThat(subscription.getGracePeriodEnd()).isEqualTo(LocalDate.now().plusDays(7));
        }
        
        @Test
        @DisplayName("Deve cancelar automaticamente após 3 falhas")
        void shouldAutoCancelAfterThreeFailures() {
            // When
            subscription.processFailedBilling(); // 1ª falha
            subscription.processFailedBilling(); // 2ª falha  
            subscription.processFailedBilling(); // 3ª falha
            
            // Then
            assertThat(subscription.isCancelled()).isTrue();
            assertThat(subscription.getFailedPaymentAttempts()).isEqualTo(3);
            assertThat(subscription.getEndDate()).isEqualTo(LocalDate.now());
        }
    }
    
    @Nested
    @DisplayName("Cálculos de Cobrança")
    class BillingCalculationTests {
        
        @Test
        @DisplayName("Deve calcular valor de cobrança por ciclo")
        void shouldCalculateBillingAmountByCycle() {
            Money baseAmount = Money.of(BigDecimal.valueOf(30.00), "BRL");
            
            // Mensal
            Subscription monthly = Subscription.create(customerId, customerEmail, planId, BillingCycle.MONTHLY, baseAmount);
            assertThat(monthly.calculateBillingAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(30.00));
            
            // Trimestral  
            Subscription quarterly = Subscription.create(customerId, customerEmail, planId, BillingCycle.QUARTERLY, baseAmount);
            assertThat(quarterly.calculateBillingAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(90.00));
            
            // Anual
            Subscription yearly = Subscription.create(customerId, customerEmail, planId, BillingCycle.YEARLY, baseAmount);
            assertThat(yearly.calculateBillingAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(360.00));
        }
        
        @Test
        @DisplayName("Deve identificar se precisa cobrança")
        void shouldIdentifyIfNeedsBilling() {
            Subscription subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount);
            subscription.activate();
            
            // Não precisa cobrança se nextBillingDate é no futuro
            assertThat(subscription.needsBilling()).isFalse();
            
            // Simular que a data de cobrança chegou (seria feito por um mock em implementação real)
            // Para teste, vamos testar a lógica diretamente
            assertThat(subscription.getNextBillingDate().isAfter(LocalDate.now())).isTrue();
        }
        
        @Test
        @DisplayName("Deve verificar período de graça")
        void shouldCheckGracePeriod() {
            Subscription subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount);
            subscription.activate();
            
            // Não está no período de graça inicialmente
            assertThat(subscription.isInGracePeriod()).isFalse();
            assertThat(subscription.isGracePeriodExpired()).isFalse();
            
            // Após primeira falha, entra em período de graça
            subscription.processFailedBilling();
            assertThat(subscription.isInGracePeriod()).isTrue();
            assertThat(subscription.isGracePeriodExpired()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Mudança de Plano")
    class PlanChangeTests {
        
        private Subscription subscription;
        
        @BeforeEach
        void setUp() {
            subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount);
            subscription.activate();
        }
        
        @Test
        @DisplayName("Deve alterar plano de assinatura ativa")
        void shouldChangePlanForActiveSubscription() {
            // Given
            String newPlanId = "ENTERPRISE_PLAN";
            Money newAmount = Money.of(BigDecimal.valueOf(99.99), "BRL");
            
            // When
            subscription.changePlan(newPlanId, newAmount);
            
            // Then
            assertThat(subscription.getPlanId()).isEqualTo(newPlanId);
            assertThat(subscription.getMonthlyAmount()).isEqualTo(newAmount);
        }
        
        @Test
        @DisplayName("Deve falhar ao alterar plano de assinatura não ativa")
        void shouldFailToChangePlanForNonActiveSubscription() {
            // Given
            subscription.cancel();
            String newPlanId = "ENTERPRISE_PLAN";
            Money newAmount = Money.of(BigDecimal.valueOf(99.99), "BRL");
            
            // When/Then
            assertThatThrownBy(() -> subscription.changePlan(newPlanId, newAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Só é possível alterar plano de assinaturas ativas");
        }
        
        @Test
        @DisplayName("Deve validar dados da mudança de plano")
        void shouldValidatePlanChangeData() {
            Money validAmount = Money.of(BigDecimal.valueOf(99.99), "BRL");
            
            // PlanId null
            assertThatThrownBy(() -> subscription.changePlan(null, validAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Novo PlanId não pode ser null ou vazio");
            
            // PlanId vazio
            assertThatThrownBy(() -> subscription.changePlan("", validAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Novo PlanId não pode ser null ou vazio");
            
            // Amount null
            assertThatThrownBy(() -> subscription.changePlan("NEW_PLAN", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Novo valor deve ser positivo");
            
            // Amount zero
            Money zeroAmount = Money.of(BigDecimal.ZERO, "BRL");
            assertThatThrownBy(() -> subscription.changePlan("NEW_PLAN", zeroAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Novo valor deve ser positivo");
        }
    }
    
    @Nested
    @DisplayName("Métodos de Conveniência")
    class ConvenienceMethodsTests {
        
        @Test
        @DisplayName("Deve verificar status corretamente")
        void shouldCheckStatusCorrectly() {
            Subscription subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount);
            
            // PENDING
            assertThat(subscription.isPending()).isTrue();
            assertThat(subscription.isActive()).isFalse();
            
            // ACTIVE
            subscription.activate();
            assertThat(subscription.isPending()).isFalse();
            assertThat(subscription.isActive()).isTrue();
            assertThat(subscription.isPaused()).isFalse();
            
            // PAUSED
            subscription.pause();
            assertThat(subscription.isActive()).isFalse();
            assertThat(subscription.isPaused()).isTrue();
            assertThat(subscription.isCancelled()).isFalse();
        }
        
        @Test
        @DisplayName("Deve implementar equals e hashCode baseado no ID")
        void shouldImplementEqualsAndHashCodeBasedOnId() {
            Subscription subscription1 = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount);
            Subscription subscription2 = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount);
            
            // IDs diferentes
            assertThat(subscription1).isNotEqualTo(subscription2);
            assertThat(subscription1.hashCode()).isNotEqualTo(subscription2.hashCode());
            
            // Mesmo objeto
            assertThat(subscription1).isEqualTo(subscription1);
            assertThat(subscription1.hashCode()).isEqualTo(subscription1.hashCode());
        }
        
        @Test
        @DisplayName("Deve ter toString informativo")
        void shouldHaveInformativeToString() {
            Subscription subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount);
            
            String toString = subscription.toString();
            
            assertThat(toString)
                .contains("Subscription")
                .contains(subscription.getId().toString())
                .contains(customerId.toString())
                .contains(planId)
                .contains("PENDING");
        }
    }
}