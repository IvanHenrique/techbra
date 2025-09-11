package com.ecommerce.platform.order.domain.enums;

/**
 * PaymentMethod - Enum
 * Métodos de pagamento suportados
 */
public enum PaymentMethod {
    CREDIT_CARD,  // Cartão de crédito
    DEBIT_CARD,   // Cartão de débito
    PIX,          // PIX (Brasil)
    BOLETO,       // Boleto bancário
    PAYPAL,       // PayPal
    DIGITAL_WALLET // Carteira digital
}