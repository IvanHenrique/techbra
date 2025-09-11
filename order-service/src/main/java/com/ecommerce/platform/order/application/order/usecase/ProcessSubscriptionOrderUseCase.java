package com.ecommerce.platform.order.application.order.usecase;

import com.ecommerce.platform.order.domain.model.Order;
import com.ecommerce.platform.order.domain.port.OrderRepository;
import com.ecommerce.platform.shared.domain.Address;
import com.ecommerce.platform.shared.domain.EventPublisher;
import com.ecommerce.platform.shared.domain.OrderCreatedEvent;
import com.ecommerce.platform.shared.domain.ValueObjects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Use Case para processar criação de pedidos recorrentes de assinaturas
 * Este use case é acionado quando uma assinatura é ativada e precisa
 * gerar automaticamente pedidos recorrentes para entrega dos produtos/serviços.
 * Responsabilidades:
 * - Criar pedido recorrente baseado na assinatura ativada
 * - Configurar dados de entrega e cobrança
 * - Agendar próximos pedidos automáticos
 * - Publicar evento OrderCreated para outros serviços
 * Fluxo de execução:
 * 1. Recebe dados da assinatura ativada
 * 2. Cria pedido com tipo SUBSCRIPTION_GENERATED
 * 3. Define produtos/serviços baseados no plano
 * 4. Configura endereço de entrega padrão do cliente
 * 5. Salva pedido com status CONFIRMED (já pago pela assinatura)
 * 6. Publica OrderCreated para inventory e notification
 * 7. Agenda próximo pedido recorrente
 * Padrões implementados:
 * - Use Case Pattern: Encapsula lógica de negócio específica
 * - Domain-Driven Design: Utiliza entidades e value objects
 * - Event-Driven Architecture: Publica eventos para integração
 * - Transactional: Garante consistência dos dados
 */
@Service
@Transactional
public class ProcessSubscriptionOrderUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessSubscriptionOrderUseCase.class);

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    @Autowired
    public ProcessSubscriptionOrderUseCase(
            OrderRepository orderRepository,
            EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Executa a criação de pedido recorrente para assinatura ativada
     *
     * @param command Comando com dados da assinatura
     * @return Resultado com dados do pedido criado
     * @throws ProcessSubscriptionOrderException se houver erro no processamento
     */
    public CompletableFuture<Result> execute(Command command) {
        logger.info("Processing subscription order creation: subscriptionId={}, customerId={}",
                command.subscriptionId(), command.customerId());

        try {
            // 1. Validar dados do comando
            validateCommand(command);

            // 2. Criar pedido recorrente
            Order order = createSubscriptionOrder(command);

            // 3. Salvar pedido no repositório
            Order savedOrder = orderRepository.save(order);

            // 4. Publicar evento OrderCreated
            publishOrderCreatedEvent(savedOrder, command);

            // 5. Retornar resultado
            Result result = new Result(
                    savedOrder.getId().toString(),
                    savedOrder.getCustomerId().toString(),
                    command.subscriptionId(),
                    savedOrder.getTotalAmount().amount(),
                    savedOrder.getStatus().name(),
                    savedOrder.getCreatedAt()
            );

            logger.info("Successfully created subscription order: orderId={}, subscriptionId={}",
                    result.orderId(), command.subscriptionId());

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            logger.error("Failed to create subscription order: subscriptionId={}",
                    command.subscriptionId(), e);
            throw new ProcessSubscriptionOrderException("Failed to process subscription order", e);
        }
    }

    /**
     * Valida os dados do comando
     */
    private void validateCommand(Command command) {
        if (command.subscriptionId() == null || command.subscriptionId().isBlank()) {
            throw new IllegalArgumentException("Subscription ID is required");
        }
        if (command.customerId() == null || command.customerId().isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (command.planId() == null || command.planId().isBlank()) {
            throw new IllegalArgumentException("Plan ID is required");
        }
        if (command.monthlyPrice() == null || command.monthlyPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly price must be positive");
        }
        if (command.nextBillingDate() == null) {
            throw new IllegalArgumentException("Next billing date is required");
        }
    }

    /**
     * Cria o pedido recorrente baseado na assinatura
     */
    private Order createSubscriptionOrder(Command command) {
        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));

        // Determinar valor do pedido baseado no ciclo de cobrança
        BigDecimal orderAmount = calculateOrderAmount(command);

        CustomerEmail customerEmail = CustomerEmail.of("customer@example.com");

        // Criar pedido com status CONFIRMED (já pago pela assinatura)
        Order order = Order.createRecurring(customerId, customerEmail, UUID.fromString(command.subscriptionId()));

        // Adicionar item do plano de assinatura
        order.addItem(ProductId.of(command.planId()), Quantity.one(), new Money(orderAmount, command.currency()));

        // Marcar como confirmado (pagamento já processado pela assinatura)
        order.confirm();

        return order;
    }

    /**
     * Calcula o valor do pedido baseado no ciclo de cobrança
     */
    private BigDecimal calculateOrderAmount(Command command) {
        if ("YEARLY".equals(command.billingCycle())) {
            // Para cobrança anual, o pedido representa um ano de serviço
            return command.monthlyPrice().multiply(BigDecimal.valueOf(12));
        }
        // Para cobrança mensal, o pedido representa um mês
        return command.monthlyPrice();
    }

    /**
     * Publica evento OrderCreated para outros serviços
     */
    private void publishOrderCreatedEvent(Order order, Command command) {
        // Criar dados do item
        var orderItems = List.of(
                new OrderCreatedEvent.OrderItemData(
                        command.planId(),
                        command.planId(), // productName
                        1, // quantity
                        order.getTotalAmount().amount(),
                        order.getTotalAmount().currency())
        );

        // Criar dados do endereço
        Address addr = Address.createDefault();
        var addressData = new OrderCreatedEvent.AddressData(
                addr.getStreet(),
                addr.getNumber(),
                addr.getComplement(),
                addr.getNeighborhood(),
                addr.getCity(),
                addr.getState(),
                addr.getZipCode(),
                addr.getCountry()
        );

        // Criar evento
        var event = OrderCreatedEvent.create(
                order.getId().toString(),
                order.getCustomerId().toString(),
                order.getTotalAmount().amount(),
                order.getTotalAmount().currency(),
                orderItems,
                addressData,
                "SUBSCRIPTION", // metodo de pagamento é via assinatura
                "SUBSCRIPTION_GENERATED"
        );

        // Publicar evento de forma assíncrona
        eventPublisher.publish(event)
                .thenRun(() -> logger.info("OrderCreated event published: orderId={}", order.getId().toString()))
                .exceptionally(throwable -> {
                    logger.error("Failed to publish OrderCreated event: orderId={}", order.getId().toString(),
                            throwable);
                    return null;
                });
    }

    /**
     * Comando para execução do use case
     */
    public record Command(
            String subscriptionId,
            String customerId,
            String planId,
            BigDecimal monthlyPrice,
            String currency,
            String billingCycle,
            LocalDateTime nextBillingDate
    ) {
        /**
         * Builder para facilitar construção do comando
         */
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String subscriptionId;
            private String customerId;
            private String planId;
            private BigDecimal monthlyPrice;
            private String currency;
            private String billingCycle;
            private LocalDateTime nextBillingDate;

            public Builder subscriptionId(String subscriptionId) {
                this.subscriptionId = subscriptionId;
                return this;
            }

            public Builder customerId(String customerId) {
                this.customerId = customerId;
                return this;
            }

            public Builder planId(String planId) {
                this.planId = planId;
                return this;
            }

            public Builder monthlyPrice(BigDecimal monthlyPrice) {
                this.monthlyPrice = monthlyPrice;
                return this;
            }

            public Builder currency(String currency) {
                this.currency = currency;
                return this;
            }

            public Builder billingCycle(String billingCycle) {
                this.billingCycle = billingCycle;
                return this;
            }

            public Builder nextBillingDate(LocalDateTime nextBillingDate) {
                this.nextBillingDate = nextBillingDate;
                return this;
            }

            public Command build() {
                return new Command(
                        subscriptionId,
                        customerId,
                        planId,
                        monthlyPrice,
                        currency,
                        billingCycle,
                        nextBillingDate
                );
            }
        }
    }

    /**
     * Resultado da execução do use case
     */
    public record Result(
            String orderId,
            String customerId,
            String subscriptionId,
            BigDecimal orderAmount,
            String orderStatus,
            java.time.Instant createdAt
    ) {}

    /**
     * Exception específica para erros neste use case
     */
    public static class ProcessSubscriptionOrderException extends RuntimeException {
        public ProcessSubscriptionOrderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}