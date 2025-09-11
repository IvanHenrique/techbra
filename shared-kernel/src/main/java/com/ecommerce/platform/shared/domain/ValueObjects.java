package com.ecommerce.platform.shared.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Value Objects compartilhados entre bounded contexts
 * Value Objects são objetos imutáveis que representam conceitos do domínio
 * sem identidade própria. São comparados por valor, não por identidade.
 * Características:
 * - Imutáveis (records)
 * - Validação na construção
 * - Semântica rica do domínio
 * - Reutilizáveis entre contextos
 */
public class ValueObjects {
    
    /**
     * Identificador único de cliente
     * Encapsula UUID e adiciona semântica de domínio
     */
    public record CustomerId(UUID value) {
        public CustomerId {
            if (value == null) {
                throw new IllegalArgumentException("Customer ID cannot be null");
            }
        }
        
        public static CustomerId generate() {
            return new CustomerId(UUID.randomUUID());
        }
        
        public static CustomerId of(String value) {
            return new CustomerId(UUID.fromString(value));
        }
        
        @Override
        public String toString() {
            return value.toString();
        }
    }
    
    /**
     * Email do cliente com validação
     * Garante que apenas emails válidos sejam aceitos
     */
    public record CustomerEmail(@Email @NotBlank String value) {
        public CustomerEmail {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Email cannot be null or empty");
            }
            
            // Validação adicional de domínio se necessária
            if (!value.toLowerCase().equals(value)) {
                value = value.toLowerCase();
            }
        }
        
        public static CustomerEmail of(String email) {
            return new CustomerEmail(email);
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    /**
     * Valor monetário com validações de domínio
     * Encapsula BigDecimal com regras de negócio
     */
    public record Money(@Positive BigDecimal amount, @NotBlank String currency) {
        public Money {
            if (amount == null) {
                throw new IllegalArgumentException("Amount cannot be null");
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
            if (currency == null || currency.trim().isEmpty()) {
                throw new IllegalArgumentException("Currency cannot be null or empty");
            }
            
            // Padroniza para 2 casas decimais
            amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        
        public static Money of(double amount) {
            return new Money(BigDecimal.valueOf(amount), "BRL");
        }
        
        public static Money of(BigDecimal amount, String currency) {
            return new Money(amount, currency);
        }
        
        public static Money zero() {
            return new Money(BigDecimal.ZERO, "BRL");
        }
        
        /**
         * Soma valores monetários
         * Valida se as moedas são compatíveis
         */
        public Money add(Money other) {
            if (!this.currency.equals(other.currency)) {
                throw new IllegalArgumentException("Cannot add different currencies");
            }
            return new Money(this.amount.add(other.amount), this.currency);
        }
        
        /**
         * Multiplica por quantidade
         */
        public Money multiply(int quantity) {
            return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
        }
        
        /**
         * Verifica se é maior que outro valor
         */
        public boolean isGreaterThan(Money other) {
            if (!this.currency.equals(other.currency)) {
                throw new IllegalArgumentException("Cannot compare different currencies");
            }
            return this.amount.compareTo(other.amount) > 0;
        }
        
        @Override
        public String toString() {
            return String.format("%s %.2f", currency, amount);
        }
    }
    
    /**
     * Status genérico para entidades
     * Encapsula estados válidos do domínio
     */
    public record Status(String value) {
        public Status {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Status cannot be null or empty");
            }
            // Normaliza para uppercase
            value = value.toUpperCase().trim();
        }
        
        public static Status of(String status) {
            return new Status(status);
        }
        
        public boolean isActive() {
            return "ACTIVE".equals(value);
        }
        
        public boolean isPending() {
            return "PENDING".equals(value);
        }
        
        public boolean isCancelled() {
            return "CANCELLED".equals(value);
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    /**
     * Identificador de produto
     * Usado tanto em pedidos quanto em estoque
     */
    public record ProductId(UUID value) {
        public ProductId {
            if (value == null) {
                throw new IllegalArgumentException("Product ID cannot be null");
            }
        }
        
        public static ProductId generate() {
            return new ProductId(UUID.randomUUID());
        }
        
        public static ProductId of(String value) {
            return new ProductId(UUID.fromString(value));
        }
        
        @Override
        public String toString() {
            return value.toString();
        }
    }
    
    /**
     * Quantidade de itens
     * Sempre positiva e com validações
     */
    public record Quantity(@Positive int value) {
        public Quantity {
            if (value <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
        }
        
        public static Quantity of(int quantity) {
            return new Quantity(quantity);
        }
        
        public static Quantity one() {
            return new Quantity(1);
        }
        
        /**
         * Soma quantidades
         */
        public Quantity add(Quantity other) {
            return new Quantity(this.value + other.value);
        }
        
        /**
         * Subtrai quantidades
         */
        public Quantity subtract(Quantity other) {
            int result = this.value - other.value;
            if (result < 0) {
                throw new IllegalArgumentException("Resulting quantity cannot be negative");
            }
            return new Quantity(result);
        }
        
        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * Identificador único de pedido
     */
    public record OrderId(UUID value) {
        public OrderId {
            if (value == null) {
                throw new IllegalArgumentException("Order ID cannot be null");
            }
        }

        public static OrderId generate() {
            return new OrderId(UUID.randomUUID());
        }

        public static OrderId of(String value) {
            return new OrderId(UUID.fromString(value));
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }
}