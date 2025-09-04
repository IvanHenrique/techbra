package com.ecommerce.platform.order.domain.port;

import com.ecommerce.platform.order.domain.model.Order;
import com.ecommerce.platform.shared.domain.ValueObjects.CustomerId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OrderRepository - Port (Interface) do Domínio
 * Define o contrato para persistência de pedidos sem depender de tecnologia específica.
 * Representa a abstração do repositório no padrão Hexagonal Architecture.
 * Características importantes:
 * - Interface pura (sem dependências de framework)
 * - Linguagem ubíqua do domínio
 * - Métodos expressivos e orientados ao negócio
 * - Não vaza detalhes de implementação
 * Esta interface será implementada pelo adapter de infrastructure
 * (ex: OrderJpaRepository) mantendo o domínio isolado.
 */
public interface OrderRepository {
    
    /**
     * Salva um pedido (insert ou update)
     * 
     * @param order Pedido a ser salvo
     * @return Pedido salvo com dados atualizados
     */
    Order save(Order order);
    
    /**
     * Busca pedido por ID
     * 
     * @param orderId ID único do pedido
     * @return Optional contendo o pedido se encontrado
     */
    Optional<Order> findById(UUID orderId);
    
    /**
     * Busca todos os pedidos de um cliente
     * Útil para histórico de pedidos e relatórios
     * 
     * @param customerId ID do cliente
     * @return Lista de pedidos do cliente (ordenados por data de criação DESC)
     */
    List<Order> findByCustomerId(CustomerId customerId);
    
    /**
     * Busca pedidos por status
     * Útil para processamento em lote e monitoramento
     * 
     * @param status Status dos pedidos buscados
     * @return Lista de pedidos com o status especificado
     */
    List<Order> findByStatus(String status);
    
    /**
     * Busca pedidos recorrentes de uma assinatura específica
     * 
     * @param subscriptionId ID da assinatura
     * @return Lista de pedidos gerados pela assinatura
     */
    List<Order> findBySubscriptionId(UUID subscriptionId);
    
    /**
     * Verifica se existe algum pedido ativo para o cliente
     * Considera pedidos nos status: PENDING, CONFIRMED, PAID, SHIPPED
     * Útil para validações de negócio
     * 
     * @param customerId ID do cliente
     * @return true se existir pelo menos um pedido ativo
     */
    boolean hasActiveOrders(CustomerId customerId);
    
    /**
     * Conta total de pedidos de um cliente
     * 
     * @param customerId ID do cliente
     * @return Número total de pedidos do cliente
     */
    long countByCustomerId(CustomerId customerId);
    
    /**
     * Busca pedidos pendentes mais antigos que X minutos
     * Útil para processamento automático de pedidos abandonados
     * 
     * @param minutesAgo Minutos atrás a partir de agora
     * @return Lista de pedidos pendentes antigos
     */
    List<Order> findPendingOrdersOlderThan(int minutesAgo);
    
    /**
     * Remove pedido por ID
     * CUIDADO: Usar apenas em casos específicos (ex: dados de teste)
     * Em produção, prefira cancelamento lógico
     * 
     * @param orderId ID do pedido a ser removido
     */
    void deleteById(UUID orderId);
    
    /**
     * Verifica se existe pedido com o ID
     * 
     * @param orderId ID do pedido
     * @return true se o pedido existe
     */
    boolean existsById(UUID orderId);
}