package com.ecommerce.platform.order.domain.model;

import com.ecommerce.platform.order.domain.enums.OrderStatus;
import com.ecommerce.platform.order.domain.enums.OrderType;
import com.ecommerce.platform.shared.domain.ValueObjects.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order - Agregado Principal do Domínio de Pedidos
 * Representa um pedido no sistema de e-commerce, podendo ser:
 * - Pedido único (one-time purchase)
 * - Pedido recorrente (gerado por assinatura)
 * Implementa padrões DDD:
 * - Aggregate Root: Controla acesso aos OrderItems
 * - Rich Domain Model: Lógica de negócio encapsulada
 * - Invariants: Regras de negócio sempre consistentes
 * - Domain Events: Comunica mudanças para outros contextos
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "id")
    private UUID id;

    // CORRIGIDO: Removido @Embedded e @AttributeOverride
    // O AttributeConverter fará a conversão automaticamente
    @Column(name = "customer_id")
    private CustomerId customerId;

    @Column(name = "customer_email")
    private CustomerEmail customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @NotNull
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    @NotNull
    private OrderType type;

    // CORRIGIDO: Removido @Embedded e @AttributeOverrides
    // Usando colunas separadas com conversão manual
    @Column(name = "total_amount")
    private java.math.BigDecimal totalAmount;

    @Column(name = "currency")
    private String currency = "BRL";

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "subscription_id")
    private UUID subscriptionId; // Nullable - apenas para pedidos recorrentes

    @Version
    @Column(name = "version")
    private Long version;

    // Relacionamento um-para-muitos com OrderItem
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    // Construtor protegido para JPA
    protected Order() {}

    /**
     * Construtor privado - Use factory methods
     */
    private Order(CustomerId customerId, CustomerEmail customerEmail, OrderType type) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.type = type;
        this.status = OrderStatus.PENDING;
        // CORRIGIDO: Definir totalAmount como BigDecimal e currency separadamente
        this.totalAmount = java.math.BigDecimal.ZERO;
        this.currency = "BRL";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version = null;
    }

    /**
     * Factory Method: Criar pedido único
     *
     * @param customerId ID do cliente
     * @param customerEmail Email do cliente
     * @return Novo pedido único
     */
    public static Order createOneTime(CustomerId customerId, CustomerEmail customerEmail) {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId não pode ser null");
        }
        if (customerEmail == null) {
            throw new IllegalArgumentException("CustomerEmail não pode ser null");
        }
        return new Order(customerId, customerEmail, OrderType.ONE_TIME);
    }

    /**
     * Factory Method: Criar pedido recorrente (gerado por assinatura)
     *
     * @param customerId ID do cliente
     * @param customerEmail Email do cliente
     * @param subscriptionId ID da assinatura que gerou o pedido
     * @return Novo pedido recorrente
     */
    public static Order createRecurring(CustomerId customerId, CustomerEmail customerEmail, UUID subscriptionId) {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId não pode ser null");
        }
        if (customerEmail == null) {
            throw new IllegalArgumentException("CustomerEmail não pode ser null");
        }
        if (subscriptionId == null) {
            throw new IllegalArgumentException("SubscriptionId não pode ser null");
        }

        Order order = new Order(customerId, customerEmail, OrderType.SUBSCRIPTION_GENERATED);
        order.subscriptionId = subscriptionId;
        return order;
    }

    /**
     * Adiciona item ao pedido
     * Regras de negócio:
     * - Pedido deve estar em status PENDING
     * - Recalcula total automaticamente
     * - Valida quantidade máxima de itens
     *
     * @param productId ID do produto
     * @param quantity Quantidade
     * @param unitPrice Preço unitário
     */
    public void addItem(ProductId productId, Quantity quantity, Money unitPrice) {
        validateCanAddItems();

        // Limite de itens por pedido (regra de negócio)
        if (items.size() >= 50) {
            throw new IllegalStateException("Ordem não pode ter mais de 50 itens");
        }

        // Verifica se produto já existe no pedido
        var existingItem = items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            // Atualiza quantidade do item existente
            existingItem.get().updateQuantity(quantity);
        } else {
            // Cria novo item
            OrderItem newItem = OrderItem.create(this, productId, quantity, unitPrice);
            items.add(newItem);
        }

        recalculateTotal();
        this.updatedAt = Instant.now();
    }

    /**
     * Remove item do pedido
     */
    public void removeItem(ProductId productId) {
        validateCanAddItems();

        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));

        if (!removed) {
            throw new IllegalArgumentException("Item não encontrado no pedido");
        }

        recalculateTotal();
        this.updatedAt = Instant.now();
    }

    /**
     * Confirma o pedido - Transição de PENDING para CONFIRMED
     * Regras de negócio:
     * - Deve ter pelo menos um item
     * - Valor total deve ser maior que zero
     * - Status deve ser PENDING
     */
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Só é possível confirmar pedidos pendentes");
        }

        if (items.isEmpty()) {
            throw new IllegalStateException("Pedido deve ter pelo menos um item");
        }

        if (totalAmount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Valor total deve ser maior que zero");
        }

        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    /**
     * Marca pedido como pago - Transição para PAID
     */
    public void markAsPaid() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Só é possível pagar pedidos confirmados");
        }

        this.status = OrderStatus.PAID;
        this.updatedAt = Instant.now();
    }

    /**
     * Marca pedido como enviado - Transição para SHIPPED
     */
    public void markAsShipped() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("Só é possível enviar pedidos pagos");
        }

        this.status = OrderStatus.SHIPPED;
        this.updatedAt = Instant.now();
    }

    /**
     * Marca pedido como entregue - Transição para DELIVERED
     */
    public void markAsDelivered() {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Só é possível entregar pedidos enviados");
        }

        this.status = OrderStatus.DELIVERED;
        this.updatedAt = Instant.now();
    }

    /**
     * Cancela o pedido
     * Regras de negócio:
     * - Só pode cancelar se não estiver DELIVERED
     */
    public void cancel() {
        if (status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Não é possível cancelar pedidos já entregues");
        }

        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Recalcula o valor total baseado nos itens
     */
    private void recalculateTotal() {
        java.math.BigDecimal total = items.stream()
                .map(OrderItem::getTotalPrice)
                .map(Money::amount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // CORRIGIDO: Atualizar totalAmount diretamente
        this.totalAmount = total;
        this.currency = "BRL";
    }

    /**
     * Valida se é possível adicionar/remover itens
     */
    private void validateCanAddItems() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Só é possível modificar pedidos pendentes");
        }
    }

    // Getters (sem setters para manter imutabilidade controlada)
    public UUID getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public CustomerEmail getCustomerEmail() { return customerEmail; }
    public OrderStatus getStatus() { return status; }
    public OrderType getType() { return type; }

    // CORRIGIDO: getTotalAmount retorna Money criado a partir dos campos separados
    public Money getTotalAmount() {
        return Money.of(totalAmount, currency);
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public Long getVersion() { return version; }
    public List<OrderItem> getItems() { return new ArrayList<>(items); } // Cópia defensiva

    /**
     * Métodos de conveniência para verificação de status
     */
    public boolean isPending() { return status == OrderStatus.PENDING; }
    public boolean isConfirmed() { return status == OrderStatus.CONFIRMED; }
    public boolean isPaid() { return status == OrderStatus.PAID; }
    public boolean isShipped() { return status == OrderStatus.SHIPPED; }
    public boolean isDelivered() { return status == OrderStatus.DELIVERED; }
    public boolean isCancelled() { return status == OrderStatus.CANCELLED; }
    public boolean isRecurring() { return type == OrderType.SUBSCRIPTION_GENERATED; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Order order = (Order) obj;
        return id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Order{id=%s, customerId=%s, status=%s, type=%s, total=%s %s}",
                id, customerId, status, type, totalAmount, currency);
    }
}