package com.ecommerce.platform.order.domain.model;

import com.ecommerce.platform.shared.domain.ValueObjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes Unitários para Order - Agregado Principal
 * Testa todas as regras de negócio e invariantes do domínio.
 * Foca na lógica de negócio sem dependências externas.
 * Organização:
 * - Testes de criação (factory methods)
 * - Testes de adição/remoção de itens
 * - Testes de transições de status
 * - Testes de regras de negócio
 * - Testes de casos excepcionais
 */
class OrderTest {
    
    private CustomerId customerId;
    private CustomerEmail customerEmail;
    private ProductId productId;
    private Quantity quantity;
    private Money unitPrice;
    
    @BeforeEach
    void setUp() {
        customerId = CustomerId.generate();
        customerEmail = CustomerEmail.of("customer@example.com");
        productId = ProductId.generate();
        quantity = Quantity.of(2);
        unitPrice = Money.of(BigDecimal.valueOf(100.00), "BRL");
    }
    
    @Nested
    @DisplayName("Criação de Pedidos")
    class OrderCreationTests {
        
        @Test
        @DisplayName("Deve criar pedido único com dados válidos")
        void shouldCreateOneTimeOrderWithValidData() {
            // When
            Order order = Order.createOneTime(customerId, customerEmail);
            
            // Then
            assertThat(order).isNotNull();
            assertThat(order.getId()).isNotNull();
            assertThat(order.getCustomerId()).isEqualTo(customerId);
            assertThat(order.getCustomerEmail()).isEqualTo(customerEmail);
            assertThat(order.getStatus().toString()).isEqualTo("PENDING");
            assertThat(order.getType().toString()).isEqualTo("ONE_TIME");
            assertThat(order.getTotalAmount()).isEqualTo(Money.zero());
            assertThat(order.getItems()).isEmpty();
            assertThat(order.getSubscriptionId()).isNull();
            assertThat(order.getVersion()).isEqualTo(null);
            assertThat(order.getCreatedAt()).isNotNull();
            assertThat(order.getUpdatedAt()).isNotNull();
        }
        
        @Test
        @DisplayName("Deve criar pedido recorrente com assinatura")
        void shouldCreateRecurringOrderWithSubscription() {
            // Given
            UUID subscriptionId = UUID.randomUUID();
            
            // When
            Order order = Order.createRecurring(customerId, customerEmail, subscriptionId);
            
            // Then
            assertThat(order).isNotNull();
            assertThat(order.getType().toString()).isEqualTo("SUBSCRIPTION_GENERATED");
            assertThat(order.getSubscriptionId()).isEqualTo(subscriptionId);
            assertThat(order.isRecurring()).isTrue();
        }

        @Test
        @DisplayName("Deve falhar ao criar pedido com dados inválidos")
        void shouldFailToCreateOrderWithInvalidData() {
            // Then
            assertThatThrownBy(() -> Order.createOneTime(null, customerEmail))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CustomerId não pode ser null");

            assertThatThrownBy(() -> Order.createOneTime(customerId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CustomerEmail não pode ser null");

            assertThatThrownBy(() -> Order.createRecurring(customerId, customerEmail, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SubscriptionId não pode ser null");
        }
    }
    
    @Nested
    @DisplayName("Gestão de Itens")
    class ItemManagementTests {
        
        private Order order;
        
        @BeforeEach
        void setUp() {
            order = Order.createOneTime(customerId, customerEmail);
        }
        
        @Test
        @DisplayName("Deve adicionar item com sucesso")
        void shouldAddItemSuccessfully() {
            // When
            order.addItem(productId, quantity, unitPrice);
            
            // Then
            assertThat(order.getItems()).hasSize(1);
            assertThat(order.getTotalAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
            
            OrderItem item = order.getItems().get(0);
            assertThat(item.getProductId()).isEqualTo(productId);
            assertThat(item.getQuantity()).isEqualTo(quantity);
            assertThat(item.getUnitPrice()).isEqualTo(unitPrice);
        }
        
        @Test
        @DisplayName("Deve atualizar quantidade quando produto já existe")
        void shouldUpdateQuantityWhenProductAlreadyExists() {
            // Given
            order.addItem(productId, quantity, unitPrice);
            Quantity newQuantity = Quantity.of(3);
            
            // When
            order.addItem(productId, newQuantity, unitPrice);
            
            // Then
            assertThat(order.getItems()).hasSize(1);
            OrderItem item = order.getItems().get(0);
            assertThat(item.getQuantity()).isEqualTo(newQuantity);
            assertThat(order.getTotalAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(300.00));
        }
        
        @Test
        @DisplayName("Deve falhar ao adicionar mais de 50 itens")
        void shouldFailToAddMoreThan50Items() {
            // Given - adicionar 50 itens diferentes
            for (int i = 0; i < 50; i++) {
                ProductId differentProduct = ProductId.generate();
                order.addItem(differentProduct, Quantity.of(1), Money.of(10.0));
            }
            
            // When/Then
            ProductId oneMoreProduct = ProductId.generate();
            assertThatThrownBy(() -> order.addItem(oneMoreProduct, Quantity.of(1), Money.of(10.0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não pode ter mais de 50 itens");
        }
        
        @Test
        @DisplayName("Deve remover item com sucesso")
        void shouldRemoveItemSuccessfully() {
            // Given
            order.addItem(productId, quantity, unitPrice);
            
            // When
            order.removeItem(productId);
            
            // Then
            assertThat(order.getItems()).isEmpty();
            assertThat(order.getTotalAmount()).isEqualTo(Money.zero());
        }
        
        @Test
        @DisplayName("Deve falhar ao remover item inexistente")
        void shouldFailToRemoveNonExistentItem() {
            // Given
            ProductId nonExistentProduct = ProductId.generate();
            
            // When/Then
            assertThatThrownBy(() -> order.removeItem(nonExistentProduct))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item não encontrado");
        }
        
        @Test
        @DisplayName("Não deve permitir modificar itens após confirmação")
        void shouldNotAllowModifyingItemsAfterConfirmation() {
            // Given
            order.addItem(productId, quantity, unitPrice);
            order.confirm();
            
            // When/Then
            ProductId anotherProduct = ProductId.generate();
            assertThatThrownBy(() -> order.addItem(anotherProduct, quantity, unitPrice))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Só é possível modificar pedidos pendentes");
                
            assertThatThrownBy(() -> order.removeItem(productId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Só é possível modificar pedidos pendentes");
        }
    }
    
    @Nested
    @DisplayName("Transições de Status")
    class StatusTransitionTests {
        
        private Order order;
        
        @BeforeEach
        void setUp() {
            order = Order.createOneTime(customerId, customerEmail);
            order.addItem(productId, quantity, unitPrice);
        }
        
        @Test
        @DisplayName("Deve confirmar pedido pendente com itens")
        void shouldConfirmPendingOrderWithItems() {
            // When
            order.confirm();
            
            // Then
            assertThat(order.isConfirmed()).isTrue();
            assertThat(order.getStatus().toString()).isEqualTo("CONFIRMED");
        }
        
        @Test
        @DisplayName("Deve falhar ao confirmar pedido sem itens")
        void shouldFailToConfirmOrderWithoutItems() {
            // Given
            Order emptyOrder = Order.createOneTime(customerId, customerEmail);
            
            // When/Then
            assertThatThrownBy(emptyOrder::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pedido deve ter pelo menos um item");
        }
        
        @Test
        @DisplayName("Deve falhar ao confirmar pedido já confirmado")
        void shouldFailToConfirmAlreadyConfirmedOrder() {
            // Given
            order.confirm();
            
            // When/Then
            assertThatThrownBy(order::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Só é possível confirmar pedidos pendentes");
        }
        
        @Test
        @DisplayName("Deve marcar como pago apenas se confirmado")
        void shouldMarkAsPaidOnlyIfConfirmed() {
            // Given
            order.confirm();
            
            // When
            order.markAsPaid();
            
            // Then
            assertThat(order.isPaid()).isTrue();
            assertThat(order.getStatus().toString()).isEqualTo("PAID");
        }
        
        @Test
        @DisplayName("Deve falhar ao marcar como pago se não confirmado")
        void shouldFailToMarkAsPaidIfNotConfirmed() {
            // When/Then
            assertThatThrownBy(order::markAsPaid)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Só é possível pagar pedidos confirmados");
        }
        
        @Test
        @DisplayName("Deve seguir fluxo completo: PENDING → CONFIRMED → PAID → SHIPPED → DELIVERED")
        void shouldFollowCompleteFlow() {
            // PENDING → CONFIRMED
            order.confirm();
            assertThat(order.isConfirmed()).isTrue();
            
            // CONFIRMED → PAID
            order.markAsPaid();
            assertThat(order.isPaid()).isTrue();
            
            // PAID → SHIPPED
            order.markAsShipped();
            assertThat(order.isShipped()).isTrue();
            
            // SHIPPED → DELIVERED
            order.markAsDelivered();
            assertThat(order.isDelivered()).isTrue();
        }
        
        @Test
        @DisplayName("Deve permitir cancelar em qualquer status exceto DELIVERED")
        void shouldAllowCancelInAnyStatusExceptDelivered() {
            // PENDING
            Order pendingOrder = Order.createOneTime(customerId, customerEmail);
            pendingOrder.addItem(productId, quantity, unitPrice);
            pendingOrder.cancel();
            assertThat(pendingOrder.isCancelled()).isTrue();
            
            // CONFIRMED
            Order confirmedOrder = Order.createOneTime(customerId, customerEmail);
            confirmedOrder.addItem(productId, quantity, unitPrice);
            confirmedOrder.confirm();
            confirmedOrder.cancel();
            assertThat(confirmedOrder.isCancelled()).isTrue();
            
            // DELIVERED - não deve permitir
            order.confirm();
            order.markAsPaid();
            order.markAsShipped();
            order.markAsDelivered();
            
            assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Não é possível cancelar pedidos já entregues");
        }
    }
    
    @Nested
    @DisplayName("Regras de Negócio")
    class BusinessRulesTests {
        
        @Test
        @DisplayName("Deve recalcular total automaticamente")
        void shouldRecalculateTotalAutomatically() {
            // Given
            Order order = Order.createOneTime(customerId, customerEmail);
            
            ProductId product1 = ProductId.generate();
            ProductId product2 = ProductId.generate();
            
            // When
            order.addItem(product1, Quantity.of(2), Money.of(50.0)); // 100.00
            order.addItem(product2, Quantity.of(1), Money.of(75.0)); // 75.00
            
            // Then
            assertThat(order.getTotalAmount().amount())
                .isEqualByComparingTo(BigDecimal.valueOf(175.00));
        }
        
        @Test
        @DisplayName("Deve identificar pedido recorrente corretamente")
        void shouldIdentifyRecurringOrderCorrectly() {
            // Given
            UUID subscriptionId = UUID.randomUUID();
            
            // When
            Order oneTimeOrder = Order.createOneTime(customerId, customerEmail);
            Order recurringOrder = Order.createRecurring(customerId, customerEmail, subscriptionId);
            
            // Then
            assertThat(oneTimeOrder.isRecurring()).isFalse();
            assertThat(recurringOrder.isRecurring()).isTrue();
        }

        @Test
        @DisplayName("Deve manter versão para controle de concorrência")
        void shouldMaintainVersionForConcurrencyControl() {
            // Given
            Order order = Order.createOneTime(customerId, customerEmail);
            Long initialVersion = order.getVersion();

            // Then
            assertThat(initialVersion).isNull(); // JPA define a versão inicial como null

            // Nota: JPA/Hibernate gerencia automaticamente a versão:
            // - null para entidades não persistidas
            // - 0 após primeiro save
            // - incrementa a cada update
        }
    }
    
    @Nested
    @DisplayName("Métodos de Conveniência")
    class ConvenienceMethodsTests {
        
        private Order order;
        
        @BeforeEach
        void setUp() {
            order = Order.createOneTime(customerId, customerEmail);
            order.addItem(productId, quantity, unitPrice);
        }
        
        @Test
        @DisplayName("Deve verificar status corretamente")
        void shouldCheckStatusCorrectly() {
            assertThat(order.isPending()).isTrue();
            assertThat(order.isConfirmed()).isFalse();
            assertThat(order.isPaid()).isFalse();
            assertThat(order.isShipped()).isFalse();
            assertThat(order.isDelivered()).isFalse();
            assertThat(order.isCancelled()).isFalse();
            
            order.confirm();
            assertThat(order.isPending()).isFalse();
            assertThat(order.isConfirmed()).isTrue();
        }
        
        @Test
        @DisplayName("Deve implementar equals e hashCode baseado no ID")
        void shouldImplementEqualsAndHashCodeBasedOnId() {
            // Given
            Order order1 = Order.createOneTime(customerId, customerEmail);
            Order order2 = Order.createOneTime(customerId, customerEmail);
            
            // Then
            assertThat(order1).isNotEqualTo(order2); // IDs diferentes
            assertThat(order1.hashCode()).isNotEqualTo(order2.hashCode());
            
            assertThat(order1).isEqualTo(order1); // Mesmo objeto
            assertThat(order1.hashCode()).isEqualTo(order1.hashCode());
        }
        
        @Test
        @DisplayName("Deve ter toString informativo")
        void shouldHaveInformativeToString() {
            // When
            String toString = order.toString();
            
            // Then
            assertThat(toString)
                .contains("Order")
                .contains(order.getId().toString())
                .contains(customerId.toString())
                .contains("PENDING")
                .contains("ONE_TIME");
        }
        
        @Test
        @DisplayName("Deve retornar cópia defensiva dos itens")
        void shouldReturnDefensiveCopyOfItems() {
            // Given
            order.addItem(productId, quantity, unitPrice);
            
            // When
            var items = order.getItems();
            
            // Then
            assertThat(items).hasSize(1);
            
            // Modificar a lista retornada não deve afetar o pedido
            items.clear();
            assertThat(order.getItems()).hasSize(1); // Original não foi modificado
        }
    }
}