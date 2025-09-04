package com.ecommerce.platform.order.adapter.out.messaging;

/**
 * Record para métricas de publicação
 * Encapsula estatísticas de publicação de eventos para monitoramento
 */
public record PublishingMetrics(
    long totalPublished,  // Total de eventos publicados com sucesso
    long totalFailed,     // Total de eventos que falharam na publicação
    long totalRetries     // Total de tentativas de retry executadas
) {
    /**
     * Factory method para métricas iniciais
     */
    public static PublishingMetrics empty() {
        return new PublishingMetrics(0L, 0L, 0L);
    }
    
    /**
     * Incrementa contador de publicações bem-sucedidas
     */
    public PublishingMetrics withPublished() {
        return new PublishingMetrics(totalPublished + 1, totalFailed, totalRetries);
    }
    
    /**
     * Incrementa contador de falhas
     */
    public PublishingMetrics withFailed() {
        return new PublishingMetrics(totalPublished, totalFailed + 1, totalRetries);
    }
    
    /**
     * Incrementa contador de retries
     */
    public PublishingMetrics withRetry() {
        return new PublishingMetrics(totalPublished, totalFailed, totalRetries + 1);
    }
    
    /**
     * Calcula taxa de sucesso
     */
    public double getSuccessRate() {
        long total = totalPublished + totalFailed;
        return total > 0 ? (double) totalPublished / total : 0.0;
    }
}