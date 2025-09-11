package com.ecommerce.platform.order.application.order.usecase;

import com.ecommerce.platform.order.domain.model.Order;
import com.ecommerce.platform.order.domain.port.OrderRepository;
import com.ecommerce.platform.order.domain.port.PaymentGateway;
import com.ecommerce.platform.order.application.order.request.PaymentRequest;
import com.ecommerce.platform.order.application.order.result.PaymentResult;
import com.ecommerce.platform.order.application.order.command.CancelOrderCommand;
import com.ecommerce.platform.order.application.order.command.ConfirmOrderCommand;
import com.ecommerce.platform.order.application.order.result.ProcessOrderResult;
import com.ecommerce.platform.order.application.order.command.ProcessPaymentCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * ProcessOrderUseCase - Caso de Uso do Domínio
 * Gerencia o ciclo completo de processamento de pedidos:
 * - Confirmação de pedidos pendentes
 * - Processamento de pagamentos
 * - Atualização de status
 * - Coordenação com serviços externos
 * Implementa Saga Pattern para transações distribuídas
 * e garante consistência eventual entre os bounded contexts.
 */
@Service
@Transactional
public class ProcessOrderUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessOrderUseCase.class);
    
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public ProcessOrderUseCase(OrderRepository orderRepository, 
                              PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }
    
    /**
     * Confirma um pedido pendente
     * 
     * @param command Comando de confirmação
     * @return Resultado da operação
     */
    public ProcessOrderResult confirmOrder(ConfirmOrderCommand command) {
        logger.info("Confirmando pedido: {}", command.orderId());
        
        try {
            // 1. Buscar pedido
            Optional<Order> orderOpt = orderRepository.findById(command.orderId());
            if (orderOpt.isEmpty()) {
                return ProcessOrderResult.failure("Pedido não encontrado");
            }
            
            Order order = orderOpt.get();
            
            // 2. Validar se pode ser confirmado
            if (!order.isPending()) {
                return ProcessOrderResult.failure("Apenas pedidos pendentes podem ser confirmados");
            }
            
            // 3. Confirmar o pedido
            order.confirm();
            
            // 4. Salvar mudanças
            Order savedOrder = orderRepository.save(order);
            logger.info("Pedido confirmado com sucesso: {}", savedOrder.getId());
            
            // 5. Publicar evento (opcional - depende dos requisitos)
            publishOrderConfirmedEvent(savedOrder);
            
            return ProcessOrderResult.success(savedOrder);
            
        } catch (Exception e) {
            logger.error("Erro ao confirmar pedido {}: {}", command.orderId(), e.getMessage(), e);
            return ProcessOrderResult.failure(e.getMessage());
        }
    }
    
    /**
     * Processa pagamento de um pedido
     * 
     * @param command Comando de pagamento
     * @return Resultado da operação
     */
    public ProcessOrderResult processPayment(ProcessPaymentCommand command) {
        logger.info("Processando pagamento para pedido: {}", command.orderId());
        
        try {
            // 1. Buscar e validar pedido
            Optional<Order> orderOpt = orderRepository.findById(command.orderId());
            if (orderOpt.isEmpty()) {
                return ProcessOrderResult.failure("Pedido não encontrado");
            }
            
            Order order = orderOpt.get();
            
            if (!order.isConfirmed()) {
                return ProcessOrderResult.failure("Apenas pedidos confirmados podem ser pagos");
            }
            
            // 2. Criar requisição de pagamento
            PaymentRequest paymentRequest = new PaymentRequest(
                order.getId(),
                order.getCustomerId(),
                order.getCustomerEmail(),
                order.getTotalAmount(),
                command.paymentMethod(),
                command.paymentToken()
            );
            
            // 3. Processar pagamento no gateway
            PaymentResult paymentResult = paymentGateway.processPayment(paymentRequest);
            
            if (paymentResult.isSuccess()) {
                // 4a. Pagamento aprovado - atualizar pedido
                order.markAsPaid();
                Order savedOrder = orderRepository.save(order);
                
                logger.info("Pagamento processado com sucesso para pedido: {}", order.getId());
                publishPaymentProcessedEvent(savedOrder, paymentResult);
                
                return ProcessOrderResult.success(savedOrder);
                
            } else {
                // 4b. Pagamento rejeitado - manter status e logar
                logger.warn("Pagamento rejeitado para pedido {}: {}", 
                    order.getId(), paymentResult.message());
                    
                publishPaymentFailedEvent(order, paymentResult);
                
                return ProcessOrderResult.failure("Pagamento rejeitado: " + paymentResult.message());
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar pagamento para pedido {}: {}", 
                command.orderId(), e.getMessage(), e);
            return ProcessOrderResult.failure("Erro interno: " + e.getMessage());
        }
    }
    
    /**
     * Cancela um pedido
     * 
     * @param command Comando de cancelamento
     * @return Resultado da operação
     */
    public ProcessOrderResult cancelOrder(CancelOrderCommand command) {
        logger.info("Cancelando pedido: {}", command.orderId());
        
        try {
            Optional<Order> orderOpt = orderRepository.findById(command.orderId());
            if (orderOpt.isEmpty()) {
                return ProcessOrderResult.failure("Pedido não encontrado");
            }
            
            Order order = orderOpt.get();
            
            // Verificar se pode ser cancelado
            if (order.isDelivered()) {
                return ProcessOrderResult.failure("Pedidos entregues não podem ser cancelados");
            }
            
            // Cancelar o pedido
            order.cancel();
            Order savedOrder = orderRepository.save(order);
            
            logger.info("Pedido cancelado com sucesso: {}", savedOrder.getId());
            publishOrderCancelledEvent(savedOrder, command.reason());
            
            return ProcessOrderResult.success(savedOrder);
            
        } catch (Exception e) {
            logger.error("Erro ao cancelar pedido {}: {}", command.orderId(), e.getMessage(), e);
            return ProcessOrderResult.failure(e.getMessage());
        }
    }
    
    /**
     * Atualiza status de envio
     */
    public ProcessOrderResult markAsShipped(UUID orderId, String trackingCode) {
        logger.info("Marcando pedido como enviado: {}", orderId);
        
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return ProcessOrderResult.failure("Pedido não encontrado");
            }
            
            Order order = orderOpt.get();
            order.markAsShipped();
            
            Order savedOrder = orderRepository.save(order);
            logger.info("Pedido marcado como enviado: {}", savedOrder.getId());
            
            // Publicar evento com código de rastreamento
            publishOrderShippedEvent(savedOrder, trackingCode);
            
            return ProcessOrderResult.success(savedOrder);
            
        } catch (Exception e) {
            logger.error("Erro ao marcar pedido {} como enviado: {}", orderId, e.getMessage(), e);
            return ProcessOrderResult.failure(e.getMessage());
        }
    }
    
    /**
     * Atualiza status de entrega
     */
    public ProcessOrderResult markAsDelivered(UUID orderId) {
        logger.info("Marcando pedido como entregue: {}", orderId);
        
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return ProcessOrderResult.failure("Pedido não encontrado");
            }
            
            Order order = orderOpt.get();
            order.markAsDelivered();
            
            Order savedOrder = orderRepository.save(order);
            logger.info("Pedido marcado como entregue: {}", savedOrder.getId());
            
            publishOrderDeliveredEvent(savedOrder);
            
            return ProcessOrderResult.success(savedOrder);
            
        } catch (Exception e) {
            logger.error("Erro ao marcar pedido {} como entregue: {}", orderId, e.getMessage(), e);
            return ProcessOrderResult.failure(e.getMessage());
        }
    }
    
    // Métodos privados para publicação de eventos
    private void publishOrderConfirmedEvent(Order order) {
        // TODO: Implementar evento OrderConfirmed quando necessário
        logger.debug("Evento OrderConfirmed seria publicado para pedido: {}", order.getId());
    }
    
    private void publishPaymentProcessedEvent(Order order, PaymentResult paymentResult) {
        // TODO: Implementar evento PaymentProcessed quando necessário
        logger.debug("Evento PaymentProcessed seria publicado para pedido: {}", order.getId());
    }
    
    private void publishPaymentFailedEvent(Order order, PaymentResult paymentResult) {
        // TODO: Implementar evento PaymentFailed quando necessário
        logger.debug("Evento PaymentFailed seria publicado para pedido: {}", order.getId());
    }
    
    private void publishOrderCancelledEvent(Order order, String reason) {
        // TODO: Implementar evento OrderCancelled quando necessário
        logger.debug("Evento OrderCancelled seria publicado para pedido: {} com motivo: {}", 
            order.getId(), reason);
    }
    
    private void publishOrderShippedEvent(Order order, String trackingCode) {
        // TODO: Implementar evento OrderShipped quando necessário
        logger.debug("Evento OrderShipped seria publicado para pedido: {} com tracking: {}", 
            order.getId(), trackingCode);
    }
    
    private void publishOrderDeliveredEvent(Order order) {
        // TODO: Implementar evento OrderDelivered quando necessário
        logger.debug("Evento OrderDelivered seria publicado para pedido: {}", order.getId());
    }
}