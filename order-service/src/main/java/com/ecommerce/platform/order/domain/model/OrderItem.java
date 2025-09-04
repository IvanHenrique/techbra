package com.ecommerce.platform.order.domain.model;

import com.ecommerce.platform.shared.domain.ValueObjects.*;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * OrderItem - Entidade do Domínio
 * Representa um item individual dentro de um pedido.
 * É uma entidade subordinada ao agregado Order.
 * Responsabilidades:
 * - Manter informações do produto no contexto do pedido
 * - Calcular preço total (quantidade × preço unitário)
 * - Validar regras de negócio dos itens
 * - Preservar dados históricos (snapshot do produto no momento da compra)
 */
@Entity
@Table(name = "order_items")
public class OrderItem {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    // Referência ao agregado pai (Order)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "product_id"))
    private ProductId productId;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "quantity"))
    private Quantity quantity;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "unit_price")),
        @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency"))
    })
    private Money unitPrice;
    
    // Dados do produto no momento da compra (snapshot para auditoria)
    @Column(name = "product_name", length = 255)
    private String productName;
    
    @Column(name = "product_description", length = 1000)
    private String productDescription;
    
    // Construtor protegido para JPA
    protected OrderItem() {}
    
    /**
     * Construtor privado - Use factory method
     */
    private OrderItem(Order order, ProductId productId, Quantity quantity, Money unitPrice) {
        this.id = UUID.randomUUID();
        this.order = order;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        
        // Validações de domínio
        validateBusinessRules();
    }
    
    /**
     * Factory Method: Criar novo item de pedido
     * 
     * @param order Pedido pai
     * @param productId ID do produto
     * @param quantity Quantidade solicitada
     * @param unitPrice Preço unitário no momento da compra
     * @return Nova instância de OrderItem
     */
    public static OrderItem create(Order order, ProductId productId, Quantity quantity, Money unitPrice) {
        if (order == null) {
            throw new IllegalArgumentException("Order não pode ser null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId não pode ser null");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity não pode ser null");  
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("UnitPrice não pode ser null");
        }
        
        return new OrderItem(order, productId, quantity, unitPrice);
    }
    
    /**
     * Atualiza a quantidade do item
     * Regras de negócio:
     * - Quantidade deve ser positiva
     * - Valida se nova quantidade é permitida
     * 
     * @param newQuantity Nova quantidade
     */
    public void updateQuantity(Quantity newQuantity) {
        if (newQuantity == null) {
            throw new IllegalArgumentException("Nova quantidade não pode ser null");
        }
        
        // Regra de negócio: quantidade máxima por item
        if (newQuantity.value() > 999) {
            throw new IllegalStateException("Quantidade máxima por item é 999");
        }
        
        this.quantity = newQuantity;
    }
    
    /**
     * Atualiza informações do produto (snapshot)
     * Usado quando há necessidade de manter dados históricos
     * do produto no momento da compra
     * 
     * @param name Nome do produto
     * @param description Descrição do produto
     */
    public void updateProductSnapshot(String name, String description) {
        this.productName = name != null ? name.trim() : null;
        this.productDescription = description != null ? description.trim() : null;
    }
    
    /**
     * Calcula o preço total do item (quantidade × preço unitário)
     * 
     * @return Preço total do item
     */
    public Money getTotalPrice() {
        return unitPrice.multiply(quantity.value());
    }
    
    /**
     * Valida regras de negócio do item
     */
    private void validateBusinessRules() {
        // Regra: Preço unitário deve ser positivo
        if (unitPrice.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Preço unitário deve ser positivo");
        }
        
        // Regra: Quantidade deve ser positiva (já validado no Value Object, mas garantia adicional)
        if (quantity.value() <= 0) {
            throw new IllegalStateException("Quantidade deve ser positiva");
        }
        
        // Regra: Quantidade máxima por item
        if (quantity.value() > 999) {
            throw new IllegalStateException("Quantidade máxima por item é 999");
        }
    }
    
    /**
     * Verifica se o item possui informações completas do produto
     * @return true se tem snapshot do produto
     */
    public boolean hasProductSnapshot() {
        return productName != null && !productName.trim().isEmpty();
    }
    
    /**
     * Calcula o desconto do item se aplicável
     * Placeholder para lógica futura de descontos
     * 
     * @return Valor do desconto (zero por padrão)
     */
    public Money getDiscountAmount() {
        // TODO: Implementar lógica de desconto quando necessário
        return Money.zero();
    }
    
    /**
     * Calcula o preço final considerando descontos
     * 
     * @return Preço final após descontos
     */
    public Money getFinalPrice() {
        Money total = getTotalPrice();
        Money discount = getDiscountAmount();
        
        // Por enquanto, sem desconto
        return total;
    }
    
    // Getters (sem setters para controlar mutabilidade)
    public UUID getId() { return id; }
    public Order getOrder() { return order; }
    public ProductId getProductId() { return productId; }
    public Quantity getQuantity() { return quantity; }
    public Money getUnitPrice() { return unitPrice; }
    public String getProductName() { return productName; }
    public String getProductDescription() { return productDescription; }
    
    /**
     * Métodos de conveniência para verificações
     */
    public boolean isQuantityGreaterThan(int value) {
        return quantity.value() > value;
    }
    
    public boolean isSameProduct(ProductId otherProductId) {
        return this.productId.equals(otherProductId);
    }
    
    public boolean isPriceGreaterThan(Money otherPrice) {
        return unitPrice.isGreaterThan(otherPrice);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderItem orderItem = (OrderItem) obj;
        return id.equals(orderItem.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("OrderItem{id=%s, productId=%s, quantity=%s, unitPrice=%s, total=%s}", 
            id, productId, quantity, unitPrice, getTotalPrice());
    }
}