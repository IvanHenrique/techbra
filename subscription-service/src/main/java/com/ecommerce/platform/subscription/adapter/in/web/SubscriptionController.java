package com.ecommerce.platform.subscription.adapter.in.web;

import com.ecommerce.platform.shared.domain.ValueObjects.CustomerId;
import com.ecommerce.platform.subscription.adapter.in.web.dto.CreateSubscriptionRequestDto;
import com.ecommerce.platform.subscription.adapter.in.web.dto.SubscriptionResponseDto;
import com.ecommerce.platform.subscription.application.subscription.command.CreateSubscriptionCommand;
import com.ecommerce.platform.subscription.application.subscription.result.CreateSubscriptionResult;
import com.ecommerce.platform.subscription.application.subscription.usecase.CreateSubscriptionUseCase;
import com.ecommerce.platform.subscription.domain.model.Subscription;
import com.ecommerce.platform.subscription.domain.port.SubscriptionRepository;
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
 */
@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscriptions", description = "API de gestão de assinaturas recorrentes")
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    private final CreateSubscriptionUseCase createSubscriptionUseCase;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionController(CreateSubscriptionUseCase createSubscriptionUseCase,
                                  SubscriptionRepository subscriptionRepository) {
        this.createSubscriptionUseCase = createSubscriptionUseCase;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Criar nova assinatura
     */
    @PostMapping
    @Operation(summary = "Criar nova assinatura")
    @ApiResponse(responseCode = "201", description = "Assinatura criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @CacheEvict(value = "customer-subscriptions", key = "#request.customerId")
    public ResponseEntity<SubscriptionResponseDto> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequestDto request) {
        logger.info("Recebendo requisição para criar assinatura para cliente: {}", request.customerId());

        try {
            CreateSubscriptionCommand command = mapToCommand(request);
            CreateSubscriptionResult result = createSubscriptionUseCase.execute(command);

            if (result.success()) {
                SubscriptionResponseDto response = mapToResponseDto(result.subscription());
                logger.info("Assinatura criada com sucesso: {}", result.subscription().getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                logger.warn("Falha ao criar assinatura: {}", result.errorMessage());
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
    public ResponseEntity<SubscriptionResponseDto> getSubscription(
            @Parameter(description = "ID da assinatura")
            @PathVariable UUID subscriptionId) {

        logger.debug("Buscando assinatura: {}", subscriptionId);

        SubscriptionResponseDto cachedResponse = getCachedSubscription(subscriptionId);

        if (cachedResponse != null) {
            return ResponseEntity.ok(cachedResponse);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Cacheable(value = "subscriptions", key = "#subscriptionId")
    public SubscriptionResponseDto getCachedSubscription(UUID subscriptionId) {
        logger.debug("Buscando assinatura no banco: {}", subscriptionId);
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        return subscriptionOpt.map(this::mapToResponseDto).orElse(null);
    }

    /**
     * Listar assinaturas de um cliente
     */
    @GetMapping
    @Operation(summary = "Listar assinaturas de um cliente")
    public ResponseEntity<List<SubscriptionResponseDto>> getCustomerSubscriptions(
            @Parameter(description = "ID do cliente")
            @RequestParam String customerId) {
        logger.debug("Buscando assinaturas do cliente: {}", customerId);
        try {
            List<SubscriptionResponseDto> cachedResponse = getCachedCustomerSubscriptions(customerId);
            return ResponseEntity.ok(cachedResponse);
        } catch (Exception e) {
            logger.error("Erro ao buscar assinaturas do cliente: {}", customerId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Cacheable(value = "customer-subscriptions", key = "#customerId")
    public List<SubscriptionResponseDto> getCachedCustomerSubscriptions(String customerId) {
        logger.debug("Buscando assinaturas do cliente no banco: {}", customerId);
        CustomerId customerIdVO = CustomerId.of(customerId);
        List<Subscription> subscriptions = subscriptionRepository.findByCustomerId(customerIdVO);

        return subscriptions.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Ativar assinatura
     */
    @PostMapping("/{subscriptionId}/activate")
    @Operation(summary = "Ativar assinatura")
    public ResponseEntity<SubscriptionResponseDto> activateSubscription(
            @PathVariable UUID subscriptionId) {

        logger.info("Ativando assinatura: {}", subscriptionId);

        try {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);

            if (subscriptionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Subscription subscription = subscriptionOpt.get();
            subscription.activate();

            Subscription savedSubscription = subscriptionRepository.save(subscription);
            SubscriptionResponseDto response = mapToResponseDto(savedSubscription);

            logger.info("Assinatura ativada com sucesso: {}", subscriptionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao ativar assinatura: {}", subscriptionId, e);
            return ResponseEntity.badRequest().body(
                    SubscriptionResponseDto.error(e.getMessage())
            );
        }
    }

    /**
     * Cancelar assinatura
     */
    @PostMapping("/{subscriptionId}/cancel")
    @Operation(summary = "Cancelar assinatura")
    @CacheEvict(value = {"subscriptions", "customer-subscriptions"}, allEntries = true)
    public ResponseEntity<SubscriptionResponseDto> cancelSubscription(
            @PathVariable UUID subscriptionId) {

        logger.info("Cancelando assinatura: {}", subscriptionId);

        try {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);

            if (subscriptionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Subscription subscription = subscriptionOpt.get();
            subscription.cancel();

            Subscription savedSubscription = subscriptionRepository.save(subscription);
            SubscriptionResponseDto response = mapToResponseDto(savedSubscription);

            logger.info("Assinatura cancelada com sucesso: {}", subscriptionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao cancelar assinatura: {}", subscriptionId, e);
            return ResponseEntity.badRequest().body(
                    SubscriptionResponseDto.error(e.getMessage())
            );
        }
    }

    // Métodos de mapeamento
    private CreateSubscriptionCommand mapToCommand(CreateSubscriptionRequestDto request) {
        return new CreateSubscriptionCommand(
                request.customerId(),
                request.customerEmail(),
                request.planId(),
                request.billingCycle(),
                request.monthlyPrice(),
                request.trialPeriodDays(),
                request.paymentMethodId()
        );
    }

    private SubscriptionResponseDto mapToResponseDto(Subscription subscription) {
        return new SubscriptionResponseDto(
                subscription.getId(),
                subscription.getCustomerId().value().toString(),
                subscription.getCustomerEmail().value(),
                subscription.getPlanId(),
                subscription.getStatus().name(),
                subscription.getBillingCycle().name(),
                subscription.getMonthlyPrice().amount(),
                subscription.getMonthlyPrice().currency(),
                subscription.getTrialPeriodDays(),
                subscription.getNextBillingDate(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt(),
                subscription.getActivatedAt(),
                subscription.getCancelledAt(),
                null, // errorMessage
                false // error
        );
    }

}