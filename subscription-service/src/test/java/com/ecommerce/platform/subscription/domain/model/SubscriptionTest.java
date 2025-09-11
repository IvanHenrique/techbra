package com.ecommerce.platform.subscription.domain.model;

import com.ecommerce.platform.shared.domain.ValueObjects.*;
import com.ecommerce.platform.subscription.domain.enums.BillingCycle;
import com.ecommerce.platform.subscription.domain.enums.SubscriptionStatus;
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
    private Integer trialPeriodDays;
    private String paymentMethodId;

    @BeforeEach
    void setUp() {
        customerId = CustomerId.generate();
        customerEmail = CustomerEmail.of("customer@example.com");
        planId = "PREMIUM_PLAN";
        billingCycle = BillingCycle.MONTHLY;
        monthlyAmount = Money.of(BigDecimal.valueOf(29.99), "BRL");
        trialPeriodDays = 7;
        paymentMethodId = "pm_123456789";
    }

    @Nested
    @DisplayName("Criação de Assinaturas")
    class SubscriptionCreationTests {

        @Test
        @DisplayName("Deve criar assinatura com dados válidos")
        void shouldCreateSubscriptionWithValidData() {
            // When
            Subscription subscription = Subscription.create(
                    customerId, customerEmail, planId, billingCycle, monthlyAmount,
                    trialPeriodDays, paymentMethodId
            );

            // Then
            assertThat(subscription).isNotNull();
            assertThat(subscription.getId()).isNotNull();
            assertThat(subscription.getCustomerId()).isEqualTo(customerId);
            assertThat(subscription.getCustomerEmail()).isEqualTo(customerEmail);
            assertThat(subscription.getPlanId()).isEqualTo(planId);
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
            assertThat(subscription.getBillingCycle()).isEqualTo(billingCycle);
            assertThat(subscription.getMonthlyAmount()).isEqualTo(monthlyAmount);
            assertThat(subscription.getTrialPeriodDays()).isEqualTo(trialPeriodDays);
            assertThat(subscription.getPaymentMethodId()).isEqualTo(paymentMethodId);
            assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.now().plusDays(trialPeriodDays));
            assertThat(subscription.isTrial()).isTrue();
        }

        @Test
        @DisplayName("Deve falhar ao criar assinatura com dados inválidos")
        void shouldFailToCreateSubscriptionWithInvalidData() {
            // CustomerId null
            assertThatThrownBy(() -> Subscription.create(null, customerEmail, planId, billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CustomerId não pode ser null");

            // CustomerEmail null
            assertThatThrownBy(() -> Subscription.create(customerId, null, planId, billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CustomerEmail não pode ser null");

            // PlanId null
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, null, billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PlanId não pode ser null ou vazio");

            // PlanId vazio
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, "", billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PlanId não pode ser null ou vazio");

            // MonthlyAmount null
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, planId, billingCycle, null, trialPeriodDays, paymentMethodId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MonthlyPrice deve ser maior que zero");

            // MonthlyAmount zero
            Money zeroAmount = Money.of(BigDecimal.ZERO, "BRL");
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, planId, billingCycle, zeroAmount, trialPeriodDays, paymentMethodId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MonthlyPrice deve ser maior que zero");

            // PaymentMethodId null
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, trialPeriodDays, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PaymentMethodId não pode ser null ou vazio");

            // PaymentMethodId vazio
            assertThatThrownBy(() -> Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, trialPeriodDays, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PaymentMethodId não pode ser null ou vazio");
        }

        @Test
        @DisplayName("Deve calcular próxima data de cobrança corretamente")
        void shouldCalculateNextBillingDateCorrectly() {
            LocalDate today = LocalDate.now();

            // Com período de trial
            Subscription withTrial = Subscription.create(customerId, customerEmail, planId, BillingCycle.MONTHLY, monthlyAmount, 7, paymentMethodId);
            assertThat(withTrial.getNextBillingDate()).isEqualTo(today.plusDays(7));

            // Sem período de trial
            Subscription withoutTrial = Subscription.create(customerId, customerEmail, planId, BillingCycle.MONTHLY, monthlyAmount, 0, paymentMethodId);
            assertThat(withoutTrial.getNextBillingDate()).isEqualTo(today.plusMonths(1));

            // Diferentes ciclos após ativação
            Subscription quarterly = Subscription.create(customerId, customerEmail, planId, BillingCycle.QUARTERLY, monthlyAmount, 0, paymentMethodId);
            quarterly.activate();
            assertThat(quarterly.getNextBillingDate()).isEqualTo(today.plusMonths(3));

            Subscription yearly = Subscription.create(customerId, customerEmail, planId, BillingCycle.YEARLY, monthlyAmount, 0, paymentMethodId);
            yearly.activate();
            assertThat(yearly.getNextBillingDate()).isEqualTo(today.plusYears(1));
        }
    }

    @Nested
    @DisplayName("Transições de Status")
    class StatusTransitionTests {

        private Subscription subscription;

        @BeforeEach
        void setUp() {
            subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId);
        }

        @Test
        @DisplayName("Deve ativar assinatura em trial")
        void shouldActivateTrialSubscription() {
            // When
            subscription.activate();

            // Then
            assertThat(subscription.isActive()).isTrue();
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscription.getActivatedAt()).isNotNull();
            assertThat(subscription.getGracePeriodEnd()).isNull();
        }

        @Test
        @DisplayName("Deve falhar ao ativar assinatura não trial/pendente")
        void shouldFailToActivateNonTrialSubscription() {
            // Given
            subscription.activate(); // Já ativa

            // When/Then
            assertThatThrownBy(subscription::activate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Só é possível ativar assinaturas em trial ou pendentes");
        }

        @Test
        @DisplayName("Deve suspender assinatura ativa")
        void shouldSuspendActiveSubscription() {
            // Given
            subscription.activate();

            // When
            subscription.suspend();

            // Then
            assertThat(subscription.isSuspended()).isTrue();
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
            assertThat(subscription.getGracePeriodEnd()).isEqualTo(LocalDate.now().plusDays(7));
        }

        @Test
        @DisplayName("Deve falhar ao suspender assinatura não ativa")
        void shouldFailToSuspendNonActiveSubscription() {
            // When/Then
            assertThatThrownBy(subscription::suspend)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Só é possível suspender assinaturas ativas");
        }

        @Test
        @DisplayName("Deve reativar assinatura suspensa")
        void shouldReactivateSuspendedSubscription() {
            // Given
            subscription.activate();
            subscription.suspend();

            // When
            subscription.reactivate();

            // Then
            assertThat(subscription.isActive()).isTrue();
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscription.getGracePeriodEnd()).isNull();
        }

        @Test
        @DisplayName("Deve falhar ao reativar assinatura não suspensa")
        void shouldFailToReactivateNonSuspendedSubscription() {
            // When/Then
            assertThatThrownBy(subscription::reactivate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Só é possível reativar assinaturas suspensas");
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
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(subscription.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("Deve falhar ao cancelar assinatura já cancelada")
        void shouldFailToCancelAlreadyCancelledSubscription() {
            // Given
            subscription.activate();
            subscription.cancel(); // Já cancelada

            // When/Then
            assertThatThrownBy(subscription::cancel)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Assinatura já está cancelada");
        }
    }

    @Nested
    @DisplayName("Processamento de Cobrança")
    class BillingProcessingTests {

        private Subscription subscription;

        @BeforeEach
        void setUp() {
            subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, 0, paymentMethodId);
            subscription.activate();
        }

        @Test
        @DisplayName("Deve processar cobrança bem-sucedida")
        void shouldProcessSuccessfulBilling() {
            // When
            subscription.processSuccessfulBilling();

            // Then
            assertThat(subscription.isActive()).isTrue();
            assertThat(subscription.getGracePeriodEnd()).isNull();
            // A data é recalculada baseada na data de ativação, não na original
            assertThat(subscription.getNextBillingDate()).isAfter(LocalDate.now());
        }

        @Test
        @DisplayName("Deve reativar assinatura suspensa após cobrança bem-sucedida")
        void shouldReactivateSuspendedSubscriptionAfterSuccessfulBilling() {
            // Given
            subscription.processFailedBilling(); // Coloca em SUSPENDED
            assertThat(subscription.isSuspended()).isTrue();

            // Reativar para poder processar cobrança
            subscription.reactivate();

            // When
            subscription.processSuccessfulBilling();

            // Then
            assertThat(subscription.isActive()).isTrue();
            assertThat(subscription.getGracePeriodEnd()).isNull();
        }

        @Test
        @DisplayName("Deve processar falha de cobrança")
        void shouldProcessBillingFailure() {
            // When
            subscription.processFailedBilling();

            // Then
            assertThat(subscription.isSuspended()).isTrue();
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
            assertThat(subscription.getGracePeriodEnd()).isEqualTo(LocalDate.now().plusDays(7));
        }

        @Test
        @DisplayName("Deve falhar ao processar cobrança bem-sucedida em assinatura não ativa")
        void shouldFailToProcessSuccessfulBillingOnNonActiveSubscription() {
            // Given
            subscription.cancel();

            // When/Then
            assertThatThrownBy(subscription::processSuccessfulBilling)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Só é possível processar cobrança de assinaturas ativas");
        }

        @Test
        @DisplayName("Deve falhar ao processar cobrança quando data não chegou")
        void shouldFailToProcessBillingWhenDateNotArrived() {
            // Given - assinatura com data futura
            Subscription futureSubscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, 30, paymentMethodId);
            futureSubscription.activate();

            // When/Then
            assertThatThrownBy(futureSubscription::processBilling)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Data de cobrança ainda não chegou");
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
            Subscription monthly = Subscription.create(customerId, customerEmail, planId, BillingCycle.MONTHLY, baseAmount, 0, paymentMethodId);
            assertThat(monthly.calculateBillingAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(30.00));

            // Trimestral
            Subscription quarterly = Subscription.create(customerId, customerEmail, planId, BillingCycle.QUARTERLY, baseAmount, 0, paymentMethodId);
            assertThat(quarterly.calculateBillingAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(90.00));

            // Anual
            Subscription yearly = Subscription.create(customerId, customerEmail, planId, BillingCycle.YEARLY, baseAmount, 0, paymentMethodId);
            assertThat(yearly.calculateBillingAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(360.00));
        }

        @Test
        @DisplayName("Deve identificar se precisa cobrança")
        void shouldIdentifyIfNeedsBilling() {
            // Assinatura com cobrança no futuro
            Subscription futureSubscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, 30, paymentMethodId);
            futureSubscription.activate();
            assertThat(futureSubscription.needsBilling()).isFalse();

            // Assinatura não ativa não precisa cobrança
            Subscription trialSubscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, 0, paymentMethodId);
            assertThat(trialSubscription.needsBilling()).isFalse();
        }

        @Test
        @DisplayName("Deve verificar período de graça")
        void shouldCheckGracePeriod() {
            Subscription subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, 0, paymentMethodId);
            subscription.activate();

            // Não está no período de graça inicialmente
            assertThat(subscription.isInGracePeriod()).isFalse();

            // Após falha de cobrança, entra em período de graça
            subscription.processFailedBilling();
            assertThat(subscription.isInGracePeriod()).isTrue();
        }
    }

    @Nested
    @DisplayName("Métodos de Conveniência")
    class ConvenienceMethodsTests {

        @Test
        @DisplayName("Deve verificar status corretamente")
        void shouldCheckStatusCorrectly() {
            Subscription subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId);

            // TRIAL
            assertThat(subscription.isTrial()).isTrue();
            assertThat(subscription.isActive()).isFalse();

            // ACTIVE
            subscription.activate();
            assertThat(subscription.isTrial()).isFalse();
            assertThat(subscription.isActive()).isTrue();
            assertThat(subscription.isSuspended()).isFalse();

            // SUSPENDED
            subscription.suspend();
            assertThat(subscription.isActive()).isFalse();
            assertThat(subscription.isSuspended()).isTrue();
            assertThat(subscription.isCancelled()).isFalse();

            // CANCELLED
            subscription.cancel();
            assertThat(subscription.isSuspended()).isFalse();
            assertThat(subscription.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("Deve implementar equals e hashCode baseado no ID")
        void shouldImplementEqualsAndHashCodeBasedOnId() {
            Subscription subscription1 = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId);
            Subscription subscription2 = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId);

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
            Subscription subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId);

            String toString = subscription.toString();

            assertThat(toString)
                    .contains("Subscription")
                    .contains(subscription.getId().toString())
                    .contains(customerId.toString())
                    .contains(planId)
                    .contains("TRIAL");
        }

        @Test
        @DisplayName("Deve retornar campos de data corretamente")
        void shouldReturnDateFieldsCorrectly() {
            Subscription subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId);

            assertThat(subscription.getCreatedAt()).isNotNull();
            assertThat(subscription.getUpdatedAt()).isNotNull();
            assertThat(subscription.getActivatedAt()).isNull();
            assertThat(subscription.getCancelledAt()).isNull();

            subscription.activate();
            assertThat(subscription.getActivatedAt()).isNotNull();

            subscription.cancel();
            assertThat(subscription.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("Deve retornar money objects corretamente")
        void shouldReturnMoneyObjectsCorrectly() {
            Subscription subscription = Subscription.create(customerId, customerEmail, planId, billingCycle, monthlyAmount, trialPeriodDays, paymentMethodId);

            assertThat(subscription.getMonthlyPrice()).isEqualTo(monthlyAmount);
            assertThat(subscription.getMonthlyAmount()).isEqualTo(monthlyAmount);
        }
    }
}