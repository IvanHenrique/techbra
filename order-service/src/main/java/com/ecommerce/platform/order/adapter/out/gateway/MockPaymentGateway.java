package com.ecommerce.platform.order.adapter.out.gateway;

import com.ecommerce.platform.order.application.order.request.PaymentRequest;
import com.ecommerce.platform.order.application.order.result.PaymentResult;
import com.ecommerce.platform.order.application.order.result.RefundResult;
import com.ecommerce.platform.order.domain.enums.PaymentStatus;
import com.ecommerce.platform.order.domain.port.*;
import com.ecommerce.platform.shared.domain.ValueObjects;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MockPaymentGateway implements PaymentGateway {
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // Mock simples que sempre aprova pagamento
        return new PaymentResult(
                true,
                "payment-" + System.currentTimeMillis(),
                PaymentStatus.COMPLETED,
                "Payment approved - MOCK",
                null // errorCode é null quando sucesso
        );
    }

    @Override
    public PaymentStatus checkPaymentStatus(String paymentId) {
        return PaymentStatus.COMPLETED; // Mock sempre retorna aprovado
    }

    @Override
    public RefundResult refundPayment(String paymentId, ValueObjects.Money amount) {
        return new RefundResult(true,
                "refund-" + System.currentTimeMillis(),
                ValueObjects.Money.of(BigDecimal.TEN, "R$"), "Refund success.");
    }

    @Override
    public boolean validatePaymentData(PaymentRequest paymentRequest) {
        return true; // Mock sempre valida como true
    }

    // Implementar outros métodos da interface PaymentGateway conforme necessário
}