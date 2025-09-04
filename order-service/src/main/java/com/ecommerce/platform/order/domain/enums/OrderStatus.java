package com.ecommerce.platform.order.domain.enums;

/**
 * Enum: Status do Pedido
 * Estados possíveis na máquina de estados do pedido
 */
public enum OrderStatus {
    PENDING,    // Criado, aguardando confirmação
    CONFIRMED,  // Confirmado, aguardando pagamento  
    PAID,       // Pago, aguardando envio
    SHIPPED,    // Enviado, aguardando entrega
    DELIVERED,  // Entregue com sucesso
    CANCELLED   // Cancelado
}