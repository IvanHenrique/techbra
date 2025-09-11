package com.ecommerce.platform.order.domain.enums;

/**
 * PaymentStatus - Enum
 * Estados possíveis de um pagamento
 */
public enum PaymentStatus {
    PENDING,     // Processando
    COMPLETED,   // Concluído com sucesso
    FAILED,      // Falhou
    CANCELLED,   // Cancelado
    REFUNDED     // Reembolsado
}