package com.ecommerce.platform.order.domain.enums;

/**
 * Enum que representa os possíveis status de um pedido
 * Define o ciclo de vida completo de um pedido no sistema,
 * desde a criação até a finalização ou cancelamento.
 * Estados do ciclo de vida:
 * 1. PENDING → Pedido criado, aguardando pagamento
 * 2. CONFIRMED → Pagamento confirmado, pedido aceito
 * 3. PROCESSING → Pedido sendo preparado/separado
 * 4. SHIPPED → Pedido enviado para entrega
 * 5. DELIVERED → Pedido entregue ao cliente
 * 6. CANCELLED → Pedido cancelado
 * 7. REFUNDED → Pedido reembolsado
 * Transições permitidas:
 * - PENDING → CONFIRMED (pagamento OK)
 * - PENDING → CANCELLED (pagamento falhou/timeout)
 * - CONFIRMED → PROCESSING (início preparação)
 * - CONFIRMED → CANCELLED (cancelamento pós-pagamento)
 * - PROCESSING → SHIPPED (envio realizado)
 * - PROCESSING → CANCELLED (problema na preparação)
 * - SHIPPED → DELIVERED (entrega confirmada)
 * - SHIPPED → CANCELLED (problema na entrega)
 * - DELIVERED → REFUNDED (devolução/estorno)
 * Padrões implementados:
 * - State Pattern: Cada status representa um estado
 * - Business Rules: Transições controladas por regras
 * - Audit Trail: Rastreamento de mudanças de estado
 */
public enum OrderStatus {
    
    /**
     * Pedido criado e aguardando confirmação de pagamento
     * Estado inicial de todos os pedidos.
     * Aguarda processamento do pagamento para prosseguir.
     * Transições possíveis:
     * - Para CONFIRMED: quando pagamento é aprovado
     * - Para CANCELLED: quando pagamento falha ou expira
     */
    PENDING("Aguardando Pagamento", "Pedido criado e aguardando confirmação de pagamento"),
    
    /**
     * Pagamento confirmado, pedido aceito para processamento
     * Pagamento foi processado com sucesso.
     * Pedido está na fila para preparação.
     * Transições possíveis:
     * - Para PROCESSING: quando inicia preparação
     * - Para CANCELLED: se cancelado após pagamento
     */
    CONFIRMED("Confirmado", "Pagamento aprovado, pedido confirmado"),
    
    /**
     * Pedido sendo preparado para envio
     * Itens sendo separados, embalados e preparados.
     * Integração com sistema de fulfillment ativa.
     * Transições possíveis:
     * - Para SHIPPED: quando enviado
     * - Para CANCELLED: se houver problema na preparação
     */
    PROCESSING("Em Preparação", "Pedido sendo preparado para envio"),
    
    /**
     * Pedido enviado para entrega
     * Saiu do centro de distribuição.
     * Código de rastreamento disponível.
     * Transições possíveis:
     * - Para DELIVERED: quando entregue
     * - Para CANCELLED: se houver problema na entrega
     */
    SHIPPED("Enviado", "Pedido enviado para entrega"),
    
    /**
     * Pedido entregue ao cliente final
     * Estado final positivo do pedido.
     * Cliente recebeu os produtos.
     * Transições possíveis:
     * - Para REFUNDED: se houver devolução/estorno
     */
    DELIVERED("Entregue", "Pedido entregue ao cliente"),
    
    /**
     * Pedido cancelado
     * Estado final negativo do pedido.
     * Pode ocorrer em qualquer fase do processo.
     * Nenhuma transição possível (estado final).
     */
    CANCELLED("Cancelado", "Pedido cancelado"),
    
    /**
     * Pedido reembolsado após entrega
     * Estado final para devoluções.
     * Cliente devolveu produtos e recebeu reembolso.
     * Nenhuma transição possível (estado final).
     */
    REFUNDED("Reembolsado", "Pedido reembolsado após devolução"),

    PAID("Pago", "Pedido pago.");

    private final String displayName;
    private final String description;

    /**
     * Constructor do enum
     * 
     * @param displayName Nome para exibição na UI
     * @param description Descrição detalhada do status
     */
    OrderStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Obtém nome para exibição
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Obtém descrição detalhada
     */
    public String getDescription() {
        return description;
    }

    /**
     * Verifica se é um status pendente (aguardando ação)
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * Verifica se é um status ativo (em processamento)
     */
    public boolean isActive() {
        return this == CONFIRMED || this == PROCESSING || this == SHIPPED;
    }

    /**
     * Verifica se é um status final (não pode mais mudar)
     */
    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED || this == REFUNDED;
    }

    /**
     * Verifica se é um status positivo (sucesso)
     */
    public boolean isSuccessful() {
        return this == DELIVERED;
    }

    /**
     * Verifica se é um status negativo (falha/cancelamento)
     */
    public boolean isUnsuccessful() {
        return this == CANCELLED || this == REFUNDED;
    }

    /**
     * Verifica se pode transicionar para outro status
     * 
     * @param newStatus Status de destino
     * @return true se a transição é permitida
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING -> newStatus == PAID || newStatus == CANCELLED;
            case PAID -> newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED -> newStatus == DELIVERED || newStatus == CANCELLED;
            case DELIVERED -> newStatus == REFUNDED;
            case CANCELLED, REFUNDED -> false; // Estados finais
        };
    }

    /**
     * Obtém próximos status possíveis
     * 
     * @return Array com status que podem ser alcançados
     */
    public OrderStatus[] getNextPossibleStatuses() {
        return switch (this) {
            case PENDING -> new OrderStatus[]{CONFIRMED, CANCELLED};
            case CONFIRMED -> new OrderStatus[]{PROCESSING, CANCELLED};
            case PROCESSING -> new OrderStatus[]{PAID, CANCELLED};
            case PAID -> new OrderStatus[]{SHIPPED, CANCELLED};
            case SHIPPED -> new OrderStatus[]{DELIVERED, CANCELLED};
            case DELIVERED -> new OrderStatus[]{REFUNDED};
            case CANCELLED, REFUNDED -> new OrderStatus[]{}; // Estados finais
        };
    }

    /**
     * Verifica se requer notificação ao cliente
     */
    public boolean requiresCustomerNotification() {
        // Todos os status exceto PROCESSING requerem notificação
        return this != PROCESSING;
    }

    /**
     * Verifica se requer atualização de estoque
     */
    public boolean requiresInventoryUpdate() {
        // Confirmar reserva ou liberar itens
        return this == CONFIRMED || this == CANCELLED;
    }

    /**
     * Obtém cor para exibição na UI (hex color)
     */
    public String getDisplayColor() {
        return switch (this) {
            case PENDING -> "#FFA500"; // Laranja
            case CONFIRMED -> "#4169E1"; // Azul royal
            case PROCESSING -> "#FFD700"; // Dourado
            case PAID -> "GREEN"; // Verde
            case SHIPPED -> "#9370DB"; // Roxo médio
            case DELIVERED -> "#32CD32"; // Verde lima
            case CANCELLED -> "#DC143C"; // Vermelho carmesim
            case REFUNDED -> "#808080"; // Cinza
        };
    }
}