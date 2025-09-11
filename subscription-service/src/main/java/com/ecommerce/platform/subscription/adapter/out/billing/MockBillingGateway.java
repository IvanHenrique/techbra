package com.ecommerce.platform.subscription.adapter.out.billing;

import com.ecommerce.platform.shared.domain.ValueObjects.Money;
import com.ecommerce.platform.subscription.application.subscription.request.BillingRequest;
import com.ecommerce.platform.subscription.application.subscription.request.BillingScheduleRequest;
import com.ecommerce.platform.subscription.application.subscription.request.BillingUpdateRequest;
import com.ecommerce.platform.subscription.application.subscription.result.BillingCancellationResult;
import com.ecommerce.platform.subscription.application.subscription.result.BillingResult;
import com.ecommerce.platform.subscription.application.subscription.result.BillingScheduleResult;
import com.ecommerce.platform.subscription.application.subscription.result.BillingUpdateResult;
import com.ecommerce.platform.subscription.domain.enums.BillingStatus;
import com.ecommerce.platform.subscription.domain.port.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Implementação Mock do BillingGateway para desenvolvimento
 * Usa as classes existentes: BillingRequest, BillingResult, BillingScheduleRequest, BillingScheduleResult
 */
@Component
public class MockBillingGateway implements BillingGateway {

    private static final Logger logger = LoggerFactory.getLogger(MockBillingGateway.class);

    @Override
    public BillingScheduleResult scheduleBilling(BillingScheduleRequest request) {
        logger.info("Mock: Agendando cobrança para assinatura {} no valor de {} {}",
                request.subscriptionId(),
                request.amount().amount(),
                request.amount().currency());

        // Simular falha em 10% dos casos
        if (shouldSimulateFailure()) {
            logger.warn("Mock: Simulando falha no agendamento para assinatura {}",
                    request.subscriptionId());
            return BillingScheduleResult.failure("Simulação de falha no agendamento");
        }

        // Simular agendamento bem-sucedido
        String billingId = "mock_billing_" + UUID.randomUUID().toString();
        LocalDate nextBillingDate = request.firstBillingDate();

        logger.info("Mock: Cobrança agendada com sucesso. Billing ID: {}", billingId);

        return BillingScheduleResult.success(billingId, nextBillingDate);
    }

    @Override
    public BillingResult processBilling(BillingRequest request) {
        logger.info("Mock: Processando cobrança para assinatura {} no valor de {} {}",
                request.subscriptionId(),
                request.amount().amount(),
                request.amount().currency());

        // Simular diferentes cenários baseado no valor
        BigDecimal amount = request.amount().amount();

        // Valores muito baixos falham (cartão negado)
        if (amount.compareTo(BigDecimal.ONE) < 0) {
            logger.warn("Mock: Simulando falha - valor muito baixo");
            return BillingResult.failure(
                    "INSUFFICIENT_FUNDS",
                    "Valor muito baixo para processar"
            );
        }

        // Valores muito altos falham (limite excedido)
        if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
            logger.warn("Mock: Simulando falha - valor muito alto");
            return BillingResult.failure(
                    "LIMIT_EXCEEDED",
                    "Valor excede limite do cartão"
            );
        }

        // Simular falha aleatória em 10% dos casos
        if (shouldSimulateFailure()) {
            logger.warn("Mock: Simulando falha aleatória para assinatura {}",
                    request.subscriptionId());
            return BillingResult.failure(
                    "PAYMENT_DECLINED",
                    "Simulação de falha aleatória"
            );
        }

        // Simular sucesso
        String transactionId = "mock_tx_" + UUID.randomUUID().toString();
        Money chargedAmount = request.amount(); // Cobrar o valor exato

        logger.info("Mock: Cobrança processada com sucesso. Transaction ID: {}", transactionId);

        return BillingResult.success(transactionId, chargedAmount);
    }

    @Override
    public BillingCancellationResult cancelBilling(UUID subscriptionId) {
        logger.info("Mock: Cancelando cobrança recorrente para assinatura: {}", subscriptionId);

        // Simular falha ocasional no cancelamento
        if (shouldSimulateFailure()) {
            logger.warn("Mock: Simulando falha no cancelamento para assinatura {}", subscriptionId);
            return BillingCancellationResult.failure("Simulação de falha no cancelamento");
        }

        // Simular cancelamento bem-sucedido
        String cancellationId = "mock_cancel_" + UUID.randomUUID().toString();
        logger.info("Mock: Cobrança cancelada com sucesso. Cancellation ID: {}", cancellationId);

        return BillingCancellationResult.success("Cobrança cancelada com sucesso");
    }

    @Override
    public BillingUpdateResult updateBilling(BillingUpdateRequest request) {
        logger.info("Mock: Atualizando cobrança para assinatura: {}", request.subscriptionId());

        // Simular falha ocasional na atualização
        if (shouldSimulateFailure()) {
            logger.warn("Mock: Simulando falha na atualização para assinatura {}", request.subscriptionId());
            return BillingUpdateResult.failure("Simulação de falha na atualização");
        }

        // Simular atualização bem-sucedida
        String updateId = "mock_update_" + UUID.randomUUID().toString();
        logger.info("Mock: Cobrança atualizada com sucesso. Update ID: {}", updateId);

        return BillingUpdateResult.success("Cobrança atualizada com sucesso");
    }

    @Override
    public BillingStatus getBillingStatus(UUID subscriptionId) {
        logger.info("Mock: Consultando status de cobrança para assinatura: {}", subscriptionId);

        // Simular diferentes status baseado no ID
        String idStr = subscriptionId.toString();
        String status;
        int failedAttempts = 0;
        String errorCode = null;
        String errorMessage = null;

        // Usar último dígito do UUID para determinar status
        char lastChar = idStr.charAt(idStr.length() - 1);

        switch (lastChar) {
            case '8' -> {
                status = "SUSPENDED";
                failedAttempts = 2;
                errorCode = "INSUFFICIENT_FUNDS";
                errorMessage = "Saldo insuficiente";
            }
            case '9' -> {
                status = "FAILED";
                failedAttempts = 3;
                errorCode = "PAYMENT_DECLINED";
                errorMessage = "Pagamento recusado pelo banco";
            }
            default -> {
                status = "ACTIVE";
            }
        }

        // Retornar apenas o enum baseado no status calculado
        switch (status) {
            case "SUSPENDED", "FAILED" -> {
                return BillingStatus.FAILED;
            }
            default -> {
                return BillingStatus.ACTIVE;
            }
        }
    }

    /**
     * Simula falha em ~10% dos casos para testar cenários de erro
     */
    private boolean shouldSimulateFailure() {
        return Math.random() < 0.1; // 10% de chance de falha
    }
}