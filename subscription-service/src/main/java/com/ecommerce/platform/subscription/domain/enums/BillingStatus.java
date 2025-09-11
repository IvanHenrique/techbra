package com.ecommerce.platform.subscription.domain.enums;

/**
 * BillingStatus - Enum
 * Estados possíveis de uma cobrança recorrente
 */
public enum BillingStatus {
    ACTIVE,      // Cobrança ativa
    PAUSED,      // Cobrança pausada
    CANCELLED,   // Cobrança cancelada
    FAILED,      // Última cobrança falhou
    PENDING      // Aguardando processamento
}