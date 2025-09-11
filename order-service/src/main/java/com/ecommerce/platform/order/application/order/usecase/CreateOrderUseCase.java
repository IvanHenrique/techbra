package com.ecommerce.platform.order.application.order.usecase;

import com.ecommerce.platform.order.domain.model.Order;
import com.ecommerce.platform.order.domain.port.OrderRepository;
import com.ecommerce.platform.order.application.order.command.CreateOrderCommand;
import com.ecommerce.platform.order.application.order.result.CreateOrderResult;
import com.ecommerce.platform.order.application.order.command.CreateRecurringOrderCommand;
import com.ecommerce.platform.order.application.order.command.OrderItemCommand;
import com.ecommerce.platform.shared.domain.Address;
import com.ecommerce.platform.shared.domain.DomainEvent;
import com.ecommerce.platform.shared.domain.EventPublisher;
import com.ecommerce.platform.shared.domain.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CreateOrderUseCase - Caso de Uso do Domínio
 * Implementa a lógica de criação de pedidos seguindo princípios DDD:
 * - Use Case puro sem dependências de framework (exceto anotações)
 * - Orquestra operações do domínio
 * - Coordena chamadas para repositories e services externos
 * - Publica eventos de domínio
 * - Mantém transações consistentes
 * Padrões aplicados:
 * - Command Pattern: Encapsula operação de criação
 * - Transaction Script: Para coordenação transacional
 * - Domain Events: Para comunicação entre contextos
 */
@Service
@Transactional
public class CreateOrderUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateOrderUseCase.class);

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    public CreateOrderUseCase(OrderRepository orderRepository, EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Executa criação de pedido único
     *
     * @param command Comando com dados do pedido
     * @return Resultado da operação
     */
    public CreateOrderResult execute(CreateOrderCommand command) {
        logger.info("Iniciando criação de pedido para cliente: {}",
                command != null ? command.customerId() : "null");

        try {
            // 1. Validações de negócio
            validateCommand(command);

            // 2. Criar o pedido
            Order order = Order.createOneTime(command.customerId(), command.customerEmail());

            // 3. Adicionar itens ao pedido
            addItemsToOrder(order, command.items());

            // 4. Salvar o pedido
            Order savedOrder = orderRepository.save(order);
            logger.info("Pedido criado com sucesso: {}", savedOrder.getId());

            // 5. Publicar evento de domínio
            publishOrderCreatedEvent(savedOrder);

            return CreateOrderResult.success(savedOrder);

        } catch (Exception e) {
            logger.error("Erro ao criar pedido: {}", e.getMessage(), e);
            return CreateOrderResult.failure(e.getMessage());
        }
    }

    /**
     * Executa criação de pedido recorrente (gerado por assinatura)
     *
     * @param command Comando com dados do pedido recorrente
     * @return Resultado da operação
     */
    public CreateOrderResult executeRecurring(CreateRecurringOrderCommand command) {
        logger.info("Iniciando criação de pedido recorrente para assinatura: {}", command.subscriptionId());

        try {
            // 1. Validações específicas para pedidos recorrentes
            validateRecurringCommand(command);

            // 2. Criar pedido recorrente
            Order order = Order.createRecurring(
                    command.customerId(),
                    command.customerEmail(),
                    command.subscriptionId()
            );

            // 3. Adicionar itens (geralmente pré-definidos pela assinatura)
            addItemsToOrder(order, command.items());

            // 4. Confirmar automaticamente (pedidos recorrentes são pré-aprovados)
            order.confirm();

            // 5. Salvar o pedido
            Order savedOrder = orderRepository.save(order);
            logger.info("Pedido recorrente criado com sucesso: {}", savedOrder.getId());

            // 6. Publicar evento
            publishOrderCreatedEvent(savedOrder);

            return CreateOrderResult.success(savedOrder);

        } catch (Exception e) {
            logger.error("Erro ao criar pedido recorrente para assinatura {}: {}",
                    command.subscriptionId(), e.getMessage(), e);
            return CreateOrderResult.failure(e.getMessage());
        }
    }

    /**
     * Valida comando de criação de pedido
     */
    private void validateCommand(CreateOrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Comando não pode ser null");
        }

        if (command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("Pedido deve ter pelo menos um item");
        }

        if (command.items().size() > 50) {
            throw new IllegalArgumentException("Pedido não pode ter mais de 50 itens");
        }

        // Validar se cliente já tem muitos pedidos pendentes
        long activeOrders = orderRepository.countByCustomerId(command.customerId());
        if (activeOrders > 10) {
            throw new IllegalStateException("Cliente possui muitos pedidos ativos");
        }
    }

    /**
     * Valida comando de pedido recorrente
     */
    private void validateRecurringCommand(CreateRecurringOrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Comando não pode ser null");
        }

        if (command.subscriptionId() == null) {
            throw new IllegalArgumentException("ID da assinatura é obrigatório para pedidos recorrentes");
        }

        if (command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("Pedido recorrente deve ter pelo menos um item");
        }
    }

    /**
     * Adiciona itens ao pedido
     */
    private void addItemsToOrder(Order order, List<OrderItemCommand> itemCommands) {
        for (OrderItemCommand itemCommand : itemCommands) {
            order.addItem(
                    itemCommand.productId(),
                    itemCommand.quantity(),
                    itemCommand.unitPrice()
            );
        }

        logger.debug("Adicionados {} itens ao pedido", itemCommands.size());
    }

    /**
     * Publica evento de pedido criado
     */
    private void publishOrderCreatedEvent(Order order) {
        try {
            DomainEvent event = OrderCreatedEvent.create(
                    order.getId().toString(),
                    order.getCustomerId().toString(),
                    order.getTotalAmount().amount(),
                    order.getTotalAmount().currency(),
                    List.of(new OrderCreatedEvent.OrderItemData(
                            "product-123", // productId
                            "Produto Teste", // productName
                            1, // quantity
                            order.getTotalAmount().amount(),
                            order.getTotalAmount().currency()
                    )), // orderItems
                    convertToAddressData(Address.createDefault()), // shippingAddress
                    "CREDIT_CARD", // paymentMethod
                    "ONE_TIME" // orderType
            );

            eventPublisher.publish(event);
            logger.info("Evento OrderCreated publicado para pedido: {}", order.getId());

        } catch (Exception e) {
            logger.error("Erro ao publicar evento para pedido {}: {}", order.getId(), e.getMessage(), e);
            // Não propagar erro - evento é importante mas não crítico para a operação
        }
    }

    /**
     * Converte Address para AddressData do evento
     */
    private OrderCreatedEvent.AddressData convertToAddressData(Address address) {
        return new OrderCreatedEvent.AddressData(
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState(),
                address.getZipCode(),
                address.getCountry()
        );
    }
}