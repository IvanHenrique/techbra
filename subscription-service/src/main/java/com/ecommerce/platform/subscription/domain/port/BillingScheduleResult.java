package com.ecommerce.platform.subscription.domain.port;

import java.time.LocalDate;

/**
 * BillingScheduleResult - Value Object
 * Resultado do agendamento de cobrança
 */
public record BillingScheduleResult(
    boolean success,
    String billingId,
    String message,
    LocalDate nextBillingDate
) {
    public static BillingScheduleResult success(String billingId, LocalDate nextBillingDate) {
        return new BillingScheduleResult(true, billingId, "Cobrança agendada com sucesso", nextBillingDate);
    }
    
    public static BillingScheduleResult failure(String message) {
        return new BillingScheduleResult(false, null, message, null);
    }
}