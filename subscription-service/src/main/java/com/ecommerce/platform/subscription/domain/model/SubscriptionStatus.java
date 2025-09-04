package com.ecommerce.platform.subscription.domain.model;

/**
 * Enum: Status da Assinatura
 */
public enum SubscriptionStatus {
    PENDING,    // Criada, aguardando primeiro pagamento
    ACTIVE,     // Ativa e sendo cobrada
    PAUSED,     // Pausada pelo cliente
    PAST_DUE,   // Pagamento em atraso
    CANCELLED,  // Cancelada
    EXPIRED     // Expirada (para assinaturas com prazo)
}