package com.ecommerce.platform.subscription.adapter.in.web;

import com.ecommerce.platform.shared.domain.ValueObjects.CustomerEmail;
import com.ecommerce.platform.shared.domain.ValueObjects.CustomerId;
import com.ecommerce.platform.shared.domain.ValueObjects.Money;
import com.ecommerce.platform.subscription.adapter.in.web.dto.CreateSubscriptionRequestDto;
import com.ecommerce.platform.subscription.adapter.in.web.dto.ProcessBillingResponseDto;
import com.ecommerce.platform.subscription.adapter.in.web.dto.SubscriptionResponseDto;
import com.ecommerce.platform.subscription.adapter.in.web.dto.SubscriptionStatusDto;
import com.ecommerce.platform.subscription.domain.model.Subscription;
import com.ecommerce.platform.subscription.domain.port.SubscriptionRepository;
import com.ecommerce.platform.subscription.domain.service.*;
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
 * SubscriptionController - Adapter de Entrada (Web)
 * Implementa a interface REST para operações de assinaturas.
 * Atua como um Adapter na arquitetura hexagonal, convertendo
 * requisições HTTP em chamadas para os casos de uso do domínio.
 * Responsabilidades:
 * - Receber e validar requisições HTTP
 * - Converter DTOs para comandos de domínio
 * - Invocar casos de uso apropriados
 * - Converter resultados para DTOs de resposta
 * - Tratar erros e exceções
 * - Implementar caching para performance
 */
@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscriptions", description = "API de gestão de assinaturas recorrentes")
public class SubscriptionController {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);
    
    private final CreateSubscriptionUseCase createSubscriptionUseCase;
    private final ProcessBillingUseCase processBillingUseCase;
    private final SubscriptionRepository subscriptionRepository;
    
    public SubscriptionController(CreateSubscriptionUseCase createSubscriptionUseCase,
                                ProcessBillingUseCase processBillingUseCase,
                                SubscriptionRepository subscriptionRepository) {
        this.createSubscriptionUseCase = createSubscriptionUseCase;
        this.processBillingUseCase = processBillingUseCase;
        this.subscriptionRepository = subscriptionRepository;
    }
    
    /**
     * Criar nova assinatura
     */
    @PostMapping
    @Operation(summary = "Criar nova assinatura", 
               description = "Cria uma nova assinatura recorrente para o cliente")
    @ApiResponse(responseCode = "201", description = "Assinatura criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "409", description = "Cliente já possui assinatura ativa deste plano")
    @CacheEvict(value = "customer-subscriptions", key = "#request.customerId")
    public ResponseEntity<SubscriptionResponseDto> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequestDto request) {
        logger.info("Recebendo requisição para criar assinatura para cliente: {}", request.customerId());
        
        try {
            // Converter DTO para comando de domínio
            CreateSubscriptionCommand command = mapToCreateSubscriptionCommand(request);
            
            // Executar caso de uso
            CreateSubscriptionResult result = createSubscriptionUseCase.execute(command);
            
            if (result.isSuccess()) {
                SubscriptionResponseDto response = mapToSubscriptionResponseDto(result.subscription());
                logger.info("Assinatura criada com sucesso: {}", result.subscription().getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                logger.warn("Falha ao criar assinatura: {}", result.errorMessage());
                
                // Verificar se é erro de duplicação
                if (result.errorMessage().contains("já possui assinatura ativa")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        SubscriptionResponseDto.error(result.errorMessage())
                    );
                }
                
                return ResponseEntity.badRequest().body(
                    SubscriptionResponseDto.error(result.errorMessage())
                );
            }
            
        } catch (Exception e) {
            logger.error("Erro inesperado ao criar assinatura: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                SubscriptionResponseDto.error("Erro interno do servidor")
            );
        }
    }
    
    /**
     * Buscar assinatura por ID
     */
    @GetMapping("/{subscriptionId}")
    @Operation(summary = "Buscar assinatura por ID")
    @ApiResponse(responseCode = "200", description = "Assinatura encontrada")
    @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")
    @Cacheable(value = "subscriptions", key = "#subscriptionId")
    public ResponseEntity<SubscriptionResponseDto> getSubscription(
            @Parameter(description = "ID da assinatura") 
            @PathVariable UUID subscriptionId) {
        
        logger.debug("Buscando assinatura: {}", subscriptionId);
        
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        
        if (subscriptionOpt.isPresent()) {
            SubscriptionResponseDto response = mapToSubscriptionResponseDto(subscriptionOpt.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Listar assinaturas de um cliente
     */
    @GetMapping
    @Operation(summary = "Listar assinaturas de um cliente")
    @Cacheable(value = "customer-subscriptions", key = "#customerId")
    public ResponseEntity<List<SubscriptionResponseDto>> getCustomerSubscriptions(
            @Parameter(description = "ID do cliente")
            @RequestParam String customerId) {
        
        logger.debug("Buscando assinaturas do cliente: {}", customerId);
        
        try {
            CustomerId customerIdVO = CustomerId.of(customerId);
            List<Subscription> subscriptions = subscriptionRepository.findByCustomerId(customerIdVO);
            
            List<SubscriptionResponseDto> response = subscriptions.stream()
                .map(this::mapToSubscriptionResponseDto)
                .toList();
                
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("ID de cliente inválido: {}", customerId);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Pausar assinatura
     */
    @PutMapping("/{subscriptionId}/pause")
    @Operation(summary = "Pausar assinatura ativa")
    @CacheEvict(value = {"subscriptions", "customer-subscriptions"}, allEntries = true)
    public ResponseEntity<SubscriptionResponseDto> pauseSubscription(@PathVariable UUID subscriptionId) {
        logger.info("Pausando assinatura: {}", subscriptionId);
        
        try {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
            
            if (subscriptionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Subscription subscription = subscriptionOpt.get();
            subscription.pause();
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            SubscriptionResponseDto response = mapToSubscriptionResponseDto(savedSubscription);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                SubscriptionResponseDto.error(e.getMessage())
            );
        } catch (Exception e) {
            logger.error("Erro ao pausar assinatura {}: {}", subscriptionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                SubscriptionResponseDto.error("Erro interno do servidor")
            );
        }
    }
    
    /**
     * Resumir assinatura pausada
     */
    @PutMapping("/{subscriptionId}/resume")
    @Operation(summary = "Resumir assinatura pausada")
    @CacheEvict(value = {"subscriptions", "customer-subscriptions"}, allEntries = true)
    public ResponseEntity<SubscriptionResponseDto> resumeSubscription(@PathVariable UUID subscriptionId) {
        logger.info("Resumindo assinatura: {}", subscriptionId);
        
        try {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
            
            if (subscriptionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Subscription subscription = subscriptionOpt.get();
            subscription.resume();
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            SubscriptionResponseDto response = mapToSubscriptionResponseDto(savedSubscription);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                SubscriptionResponseDto.error(e.getMessage())
            );
        } catch (Exception e) {
            logger.error("Erro ao resumir assinatura {}: {}", subscriptionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                SubscriptionResponseDto.error("Erro interno do servidor")
            );
        }
    }
    
    /**
     * Cancelar assinatura
     */
    @DeleteMapping("/{subscriptionId}")
    @Operation(summary = "Cancelar assinatura")
    @CacheEvict(value = {"subscriptions", "customer-subscriptions"}, allEntries = true)
    public ResponseEntity<SubscriptionResponseDto> cancelSubscription(@PathVariable UUID subscriptionId) {
        logger.info("Cancelando assinatura: {}", subscriptionId);
        
        try {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
            
            if (subscriptionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Subscription subscription = subscriptionOpt.get();
            subscription.cancel();
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            SubscriptionResponseDto response = mapToSubscriptionResponseDto(savedSubscription);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                SubscriptionResponseDto.error(e.getMessage())
            );
        } catch (Exception e) {
            logger.error("Erro ao cancelar assinatura {}: {}", subscriptionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                SubscriptionResponseDto.error("Erro interno do servidor")
            );
        }
    }
    
    /**
     * Processar cobrança manual (para testes/admin)
     */
    @PostMapping("/{subscriptionId}/billing/process")
    @Operation(summary = "Processar cobrança manual", 
               description = "Força processamento de cobrança para uma assinatura específica")
    public ResponseEntity<ProcessBillingResponseDto> processBilling(@PathVariable UUID subscriptionId) {
        logger.info("Processando cobrança manual para assinatura: {}", subscriptionId);
        
        ProcessBillingResult result = processBillingUseCase.processSingleSubscriptionBilling(subscriptionId);
        
        ProcessBillingResponseDto response = new ProcessBillingResponseDto(
            result.isSuccess(),
            result.message(),
            result.successCount(),
            result.failureCount()
        );
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Status simplificado da assinatura
     */
    @GetMapping("/{subscriptionId}/status")
    @Operation(summary = "Obter status da assinatura")
    public ResponseEntity<SubscriptionStatusDto> getSubscriptionStatus(@PathVariable UUID subscriptionId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            SubscriptionStatusDto status = new SubscriptionStatusDto(
                subscription.getId(),
                subscription.getStatus().toString(),
                subscription.getNextBillingDate(),
                subscription.isInGracePeriod(),
                subscription.getFailedPaymentAttempts()
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
    private CreateSubscriptionCommand mapToCreateSubscriptionCommand(CreateSubscriptionRequestDto request) {
        var customerId = CustomerId.of(request.customerId());
        var customerEmail = CustomerEmail.of(request.customerEmail());
        var monthlyAmount = Money.of(request.monthlyAmount(), "BRL");
        
        return new CreateSubscriptionCommand(
            customerId,
            customerEmail,
            request.planId(),
            request.billingCycle(),
            monthlyAmount,
            request.paymentMethodToken()
        );
    }
    
    /**
     * Mapeia assinatura de domínio para DTO de resposta
     */
    private SubscriptionResponseDto mapToSubscriptionResponseDto(Subscription subscription) {
        return new SubscriptionResponseDto(
            subscription.getId(),
            subscription.getCustomerId().value(),
            subscription.getCustomerEmail().value(),
            subscription.getPlanId(),
            subscription.getStatus().toString(),
            subscription.getBillingCycle().toString(),
            subscription.getMonthlyAmount().amount(),
            subscription.getMonthlyAmount().currency(),
            subscription.getStartDate(),
            subscription.getNextBillingDate(),
            subscription.getEndDate(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt(),
            subscription.getGracePeriodEnd(),
            subscription.getFailedPaymentAttempts(),
            null // errorMessage
        );
    }
}