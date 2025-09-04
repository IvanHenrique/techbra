package com.ecommerce.platform.order.adapter.in.web;

import com.ecommerce.platform.order.adapter.in.web.dto.*;
import com.ecommerce.platform.order.domain.service.CreateOrderUseCase;
import com.ecommerce.platform.order.domain.service.CreateOrderCommand;
import com.ecommerce.platform.order.domain.service.CreateOrderResult;
import com.ecommerce.platform.order.domain.service.ProcessOrderUseCase;
import com.ecommerce.platform.order.domain.service.ProcessOrderResult;
import com.ecommerce.platform.order.domain.service.ConfirmOrderCommand;
import com.ecommerce.platform.order.domain.service.ProcessPaymentCommand;
import com.ecommerce.platform.order.domain.service.CancelOrderCommand;
import com.ecommerce.platform.order.domain.port.OrderRepository;
import com.ecommerce.platform.order.domain.model.Order;
import com.ecommerce.platform.shared.domain.ValueObjects.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OrderController - Adapter de Entrada (Web)
 * Implementa a interface REST para operações de pedidos.
 * Atua como um Adapter na arquitetura hexagonal, convertendo
 * requisições HTTP em chamadas para os casos de uso do domínio.
 * Responsabilidades:
 * - Receber e validar requisições HTTP
 * - Converter DTOs para comandos de domínio
 * - Invocar casos de uso apropriados
 * - Converter resultados para DTOs de resposta
 * - Tratar erros e exceções
 * - Implementar caching quando apropriado
 * Padrões aplicados:
 * - Adapter Pattern: Adapta HTTP para domínio
 * - DTO Pattern: Separação entre API e domínio
 * - Controller Pattern: Coordenação de operações web
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "API de gestão de pedidos")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final CreateOrderUseCase createOrderUseCase;
    private final ProcessOrderUseCase processOrderUseCase;
    private final OrderRepository orderRepository;
    
    public OrderController(CreateOrderUseCase createOrderUseCase,
                          ProcessOrderUseCase processOrderUseCase,
                          OrderRepository orderRepository) {
        this.createOrderUseCase = createOrderUseCase;
        this.processOrderUseCase = processOrderUseCase;
        this.orderRepository = orderRepository;
    }
    
    /**
     * Criar novo pedido
     */
    @PostMapping
    @Operation(summary = "Criar novo pedido", 
               description = "Cria um novo pedido com os itens especificados")
    @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @CacheEvict(value = "customer-orders", key = "#request.customerId")
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody CreateOrderRequestDto request) {
        logger.info("Recebendo requisição para criar pedido para cliente: {}", request.customerId());
        
        try {
            // Converter DTO para comando de domínio
            CreateOrderCommand command = mapToCreateOrderCommand(request);
            
            // Executar caso de uso
            CreateOrderResult result = createOrderUseCase.execute(command);
            
            if (result.isSuccess()) {
                OrderResponseDto response = mapToOrderResponseDto(result.order());
                logger.info("Pedido criado com sucesso: {}", result.order().getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                logger.warn("Falha ao criar pedido: {}", result.errorMessage());
                return ResponseEntity.badRequest().body(
                    OrderResponseDto.error(result.errorMessage())
                );
            }
            
        } catch (Exception e) {
            logger.error("Erro inesperado ao criar pedido: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                OrderResponseDto.error("Erro interno do servidor")
            );
        }
    }
    
    /**
     * Buscar pedido por ID
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Buscar pedido por ID")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    @Cacheable(value = "orders", key = "#orderId")
    public ResponseEntity<OrderResponseDto> getOrder(
            @Parameter(description = "ID do pedido") 
            @PathVariable UUID orderId) {
        
        logger.debug("Buscando pedido: {}", orderId);
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        
        if (orderOpt.isPresent()) {
            OrderResponseDto response = mapToOrderResponseDto(orderOpt.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Listar pedidos de um cliente
     */
    @GetMapping
    @Operation(summary = "Listar pedidos de um cliente")
    @Cacheable(value = "customer-orders", key = "#customerId")
    public ResponseEntity<List<OrderResponseDto>> getCustomerOrders(
            @Parameter(description = "ID do cliente")
            @RequestParam String customerId) {
        
        logger.debug("Buscando pedidos do cliente: {}", customerId);
        
        try {
            CustomerId customerIdVO = CustomerId.of(customerId);
            List<Order> orders = orderRepository.findByCustomerId(customerIdVO);
            
            List<OrderResponseDto> response = orders.stream()
                .map(this::mapToOrderResponseDto)
                .toList();
                
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("ID de cliente inválido: {}", customerId);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Confirmar pedido
     */
    @PutMapping("/{orderId}/confirm")
    @Operation(summary = "Confirmar pedido pendente")
    @CacheEvict(value = {"orders", "customer-orders"}, allEntries = true)
    public ResponseEntity<OrderResponseDto> confirmOrder(@PathVariable UUID orderId) {
        logger.info("Confirmando pedido: {}", orderId);
        
        ConfirmOrderCommand command = new ConfirmOrderCommand(orderId);
        ProcessOrderResult result = processOrderUseCase.confirmOrder(command);
        
        if (result.isSuccess()) {
            OrderResponseDto response = mapToOrderResponseDto(result.order());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(
                OrderResponseDto.error(result.errorMessage())
            );
        }
    }
    
    /**
     * Processar pagamento
     */
    @PutMapping("/{orderId}/payment")
    @Operation(summary = "Processar pagamento do pedido")
    @CacheEvict(value = {"orders", "customer-orders"}, allEntries = true)
    public ResponseEntity<OrderResponseDto> processPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody ProcessPaymentRequestDto request) {
        
        logger.info("Processando pagamento para pedido: {}", orderId);
        
        try {
            ProcessPaymentCommand command = new ProcessPaymentCommand(
                orderId,
                request.paymentMethod(),
                request.paymentToken()
            );
            
            ProcessOrderResult result = processOrderUseCase.processPayment(command);
            
            if (result.isSuccess()) {
                OrderResponseDto response = mapToOrderResponseDto(result.order());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(
                    OrderResponseDto.error(result.errorMessage())
                );
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar pagamento para pedido {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                OrderResponseDto.error("Erro interno do servidor")
            );
        }
    }
    
    /**
     * Cancelar pedido
     */
    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancelar pedido")
    @CacheEvict(value = {"orders", "customer-orders"}, allEntries = true)
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable UUID orderId,
            @RequestParam String reason) {
        
        logger.info("Cancelando pedido: {} com motivo: {}", orderId, reason);
        
        CancelOrderCommand command = new CancelOrderCommand(orderId, reason);
        ProcessOrderResult result = processOrderUseCase.cancelOrder(command);
        
        if (result.isSuccess()) {
            OrderResponseDto response = mapToOrderResponseDto(result.order());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(
                OrderResponseDto.error(result.errorMessage())
            );
        }
    }
    
    /**
     * Marcar como enviado
     */
    @PutMapping("/{orderId}/ship")
    @Operation(summary = "Marcar pedido como enviado")
    @CacheEvict(value = {"orders", "customer-orders"}, allEntries = true)
    public ResponseEntity<OrderResponseDto> markAsShipped(
            @PathVariable UUID orderId,
            @RequestParam String trackingCode) {
        
        logger.info("Marcando pedido {} como enviado com tracking: {}", orderId, trackingCode);
        
        ProcessOrderResult result = processOrderUseCase.markAsShipped(orderId, trackingCode);
        
        if (result.isSuccess()) {
            OrderResponseDto response = mapToOrderResponseDto(result.order());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(
                OrderResponseDto.error(result.errorMessage())
            );
        }
    }
    
    /**
     * Status do pedido (endpoint simplificado)
     */
    @GetMapping("/{orderId}/status")
    @Operation(summary = "Obter apenas o status do pedido")
    public ResponseEntity<OrderStatusDto> getOrderStatus(@PathVariable UUID orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            OrderStatusDto status = new OrderStatusDto(
                order.getId(),
                order.getStatus().toString(),
                order.getUpdatedAt()
            );
            return ResponseEntity.ok(status);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // ===== MÉTODOS PRIVADOS DE MAPEAMENTO =====
    
    /**
     * Mapeia DTO de request para comando de domínio
     */
    private CreateOrderCommand mapToCreateOrderCommand(CreateOrderRequestDto request) {
        var customerId = CustomerId.of(request.customerId());
        var customerEmail = CustomerEmail.of(request.customerEmail());
        
        var items = request.items().stream()
            .map(itemDto -> new com.ecommerce.platform.order.domain.service.OrderItemCommand(
                ProductId.of(itemDto.productId()),
                Quantity.of(itemDto.quantity()),
                Money.of(itemDto.unitPrice(), "BRL")
            ))
            .toList();
            
        return new CreateOrderCommand(customerId, customerEmail, items);
    }
    
    /**
     * Mapeia ordem de domínio para DTO de resposta
     */
    private OrderResponseDto mapToOrderResponseDto(Order order) {
        var items = order.getItems().stream()
            .map(item -> new OrderItemResponseDto(
                item.getId(),
                item.getProductId().value(),
                item.getQuantity().value(),
                item.getUnitPrice().amount(),
                item.getTotalPrice().amount(),
                item.getProductName(),
                item.getProductDescription()
            ))
            .toList();
            
        return new OrderResponseDto(
            order.getId(),
            order.getCustomerId().value(),
            order.getCustomerEmail().value(),
            order.getStatus().toString(),
            order.getType().toString(),
            order.getTotalAmount().amount(),
            order.getTotalAmount().currency(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getSubscriptionId(),
            items,
            null // errorMessage
        );
    }
}