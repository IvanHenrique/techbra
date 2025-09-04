package com.ecommerce.platform.subscription.domain.port;

/**
 * BillingStatus - Enum
 * Estados possíveis de uma cobrança recorrente
 */
enum BillingStatus {
    ACTIVE,      // Cobrança ativa
    PAUSED,      // Cobrança pausada
    CANCELLED,   // Cobrança cancelada
    FAILED,      // Última cobrança falhou
    PENDING      // Aguardando processamento
}