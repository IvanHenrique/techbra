package com.ecommerce.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Evento de domínio disparado quando um pedido é criado
 * Este evento marca o início da saga de processamento de pedidos e
 * é consumido por vários serviços para coordenar o fluxo:
 * Fluxo de processamento:
 * 1. Order Service: Cria pedido → OrderCreated
 * 2. Payment Service: Processa pagamento
 * 3. Inventory Service: Reserva produtos
 * 4. Notification Service: Notifica cliente
 * Consumers deste evento:
 * - Payment Service: Para processar pagamento
 * - Inventory Service: Para reservar itens
 * - Notification Service: Para confirmar pedido
 * - Analytics Service: Para métricas de vendas
 * Padrões implementados:
 * - Event Sourcing: Representa mudança de estado
 * - Saga Pattern: Inicia coordenação distribuída
 * - CQRS: Trigger para atualizações de read models
 */
public record OrderCreatedEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("occurredAt") LocalDateTime occurredAt,
        @JsonProperty("aggregateId") String aggregateId,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("customerId") String customerId,
        @JsonProperty("orderTotal") BigDecimal orderTotal,
        @JsonProperty("currency") String currency,
        @JsonProperty("orderItems") List<OrderItemData> orderItems,
        @JsonProperty("shippingAddress") AddressData shippingAddress,
        @JsonProperty("paymentMethod") String paymentMethod,
        @JsonProperty("orderType") String orderType
) implements DomainEvent {

    /**
     * Constructor para criação via Jackson (deserialização JSON)
     */
    @JsonCreator
    public OrderCreatedEvent {
        // Validações básicas para garantir integridade do evento
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at cannot be null");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or blank");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        if (orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Order total must be non-negative");
        }
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }

    // ===== IMPLEMENTAÇÃO DOS MÉTODOS DA INTERFACE DomainEvent =====

    /**
     * Implementação explícita do método getEventId() da interface
     */
    @Override
    public UUID getEventId() {
        return eventId;
    }

    /**
     * Implementação explícita do método getOccurredAt() da interface
     */
    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    /**
     * Implementação explícita do método getAggregateId() da interface
     */
    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    /**
     * Implementação explícita do método getEventType() da interface
     */
    @Override
    public String getEventType() {
        return "ORDER_CREATED";
    }

    // ===== MÉTODOS DE NEGÓCIO ESPECÍFICOS =====

    /**
     * Factory method para criação a partir de dados de domínio
     */
    public static OrderCreatedEvent create(
            String orderId,
            String customerId,
            BigDecimal orderTotal,
            String currency,
            List<OrderItemData> orderItems,
            AddressData shippingAddress,
            String paymentMethod,
            String orderType) {

        return new OrderCreatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                orderId, // aggregateId é o próprio orderId
                orderId,
                customerId,
                orderTotal,
                currency,
                List.copyOf(orderItems), // Cópia imutável
                shippingAddress,
                paymentMethod,
                orderType
        );
    }

    /**
     * Calcula o valor total dos itens (para validação)
     */
    public BigDecimal calculateItemsTotal() {
        return orderItems.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Verifica se é um pedido de assinatura
     */
    public boolean isSubscriptionOrder() {
        return "SUBSCRIPTION_GENERATED".equals(orderType);
    }

    /**
     * Verifica se é um pedido único
     */
    public boolean isOneTimeOrder() {
        return "ONE_TIME".equals(orderType);
    }

    /**
     * Obtém IDs dos produtos para reserva de estoque
     */
    public List<String> getProductIds() {
        return orderItems.stream()
                .map(OrderItemData::productId)
                .toList();
    }

    /**
     * Verifica se é um pedido de alto valor (>= R$ 500)
     */
    public boolean isHighValueOrder() {
        return orderTotal.compareTo(new BigDecimal("500.00")) >= 0;
    }

    /**
     * Obtém quantidade total de itens
     */
    public int getTotalItemsQuantity() {
        return orderItems.stream()
                .mapToInt(OrderItemData::quantity)
                .sum();
    }

    /**
     * Verifica se precisa de aprovação manual
     */
    public boolean requiresManualApproval() {
        return isHighValueOrder() && "CREDIT_CARD".equals(paymentMethod);
    }

    /**
     * Verifica se o pagamento é via PIX
     */
    public boolean isPixPayment() {
        return "PIX".equals(paymentMethod);
    }

    /**
     * Verifica se o pagamento é via cartão de crédito
     */
    public boolean isCreditCardPayment() {
        return "CREDIT_CARD".equals(paymentMethod);
    }

    /**
     * Verifica se o pagamento é via boleto
     */
    public boolean isBoletoPayment() {
        return "BOLETO".equals(paymentMethod);
    }

    /**
     * Record para dados de item do pedido
     * Contém informações necessárias para:
     * - Reserva de estoque
     * - Cálculo de preços
     * - Geração de nota fiscal
     */
    public record OrderItemData(
            @JsonProperty("productId") String productId,
            @JsonProperty("productName") String productName,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("unitPrice") BigDecimal unitPrice,
            @JsonProperty("currency") String currency
    ) {
        @JsonCreator
        public OrderItemData {
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("Product ID cannot be null or blank");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price must be non-negative");
            }
        }

        /**
         * Calcula o subtotal do item
         */
        public BigDecimal getSubtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /**
     * Record para dados de endereço
     * Utilizado para:
     * - Cálculo de frete
     * - Validação de área de entrega
     * - Integração com transportadoras
     */
    public record AddressData(
            @JsonProperty("street") String street,
            @JsonProperty("number") String number,
            @JsonProperty("complement") String complement,
            @JsonProperty("neighborhood") String neighborhood,
            @JsonProperty("city") String city,
            @JsonProperty("state") String state,
            @JsonProperty("zipCode") String zipCode,
            @JsonProperty("country") String country
    ) {
        @JsonCreator
        public AddressData {
            if (street == null || street.isBlank()) {
                throw new IllegalArgumentException("Street cannot be null or blank");
            }
            if (city == null || city.isBlank()) {
                throw new IllegalArgumentException("City cannot be null or blank");
            }
            if (zipCode == null || zipCode.isBlank()) {
                throw new IllegalArgumentException("Zip code cannot be null or blank");
            }
        }

        /**
         * Retorna endereço formatado para exibição
         */
        public String getFormattedAddress() {
            StringBuilder sb = new StringBuilder();
            sb.append(street).append(", ").append(number);
            if (complement != null && !complement.isBlank()) {
                sb.append(" - ").append(complement);
            }
            sb.append(", ").append(neighborhood);
            sb.append(", ").append(city).append(" - ").append(state);
            sb.append(", ").append(zipCode);
            return sb.toString();
        }
    }
}