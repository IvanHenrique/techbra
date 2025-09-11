package com.ecommerce.platform.subscription.domain.enums;

/**
 * Status possíveis de uma assinatura
 */
public enum SubscriptionStatus {
    TRIAL("Trial"),           // Período de teste
    PENDING("Pending"),       // Aguardando primeiro pagamento
    ACTIVE("Active"),         // Ativa e cobrando
    SUSPENDED("Suspended"),   // Suspensa por falta de pagamento
    CANCELLED("Cancelled");   // Cancelada pelo cliente ou sistema
    
    private final String displayName;
    
    SubscriptionStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}