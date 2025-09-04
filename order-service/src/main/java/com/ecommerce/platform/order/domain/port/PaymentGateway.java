package com.ecommerce.platform.order.domain.port;

import com.ecommerce.platform.shared.domain.ValueObjects.Money;

/**
 * PaymentGateway - Port (Interface) do Domínio
 * Define o contrato para integração com serviços de pagamento externos
 * sem depender de implementação específica (Stripe, PayPal, etc).
 * Responsabilidades:
 * - Abstrair complexidades de diferentes gateways de pagamento
 * - Manter linguagem ubíqua do domínio
 * - Não vazar detalhes técnicos da integração
 * - Permitir testabilidade através de mocks
 */
public interface PaymentGateway {
    
    /**
     * Processa pagamento para um pedido
     * 
     * @param paymentRequest Dados do pagamento
     * @return Resultado do processamento
     */
    PaymentResult processPayment(PaymentRequest paymentRequest);
    
    /**
     * Verifica status de um pagamento já processado
     * 
     * @param paymentId ID do pagamento no gateway
     * @return Status atual do pagamento
     */
    PaymentStatus checkPaymentStatus(String paymentId);
    
    /**
     * Cancela/reembolsa um pagamento
     * 
     * @param paymentId ID do pagamento
     * @param amount Valor a ser reembolsado (pode ser parcial)
     * @return Resultado do reembolso
     */
    RefundResult refundPayment(String paymentId, Money amount);
    
    /**
     * Valida dados de pagamento antes do processamento
     * 
     * @param paymentRequest Dados do pagamento
     * @return true se os dados são válidos
     */
    boolean validatePaymentData(PaymentRequest paymentRequest);
}