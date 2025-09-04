package com.ecommerce.platform.order.domain.port;

/**
 * PaymentStatus - Enum
 * Estados possíveis de um pagamento
 */
enum PaymentStatus {
    PENDING,     // Processando
    COMPLETED,   // Concluído com sucesso
    FAILED,      // Falhou
    CANCELLED,   // Cancelado
    REFUNDED     // Reembolsado
}