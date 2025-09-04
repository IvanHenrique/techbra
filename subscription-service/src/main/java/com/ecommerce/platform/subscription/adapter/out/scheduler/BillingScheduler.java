package com.ecommerce.platform.subscription.adapter.out.scheduler;

import com.ecommerce.platform.subscription.domain.service.ProcessBillingUseCase;
import com.ecommerce.platform.subscription.domain.service.ProcessBillingResult;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * BillingScheduler - Job Scheduler para Cobrança Recorrente
 * Implementa jobs do Quartz para processar automaticamente:
 * - Cobranças diárias de assinaturas que vencem
 * - Retry de pagamentos falhados
 * - Cancelamento automático após período de graça
 * Executa de forma resiliente e com logging detalhado
 * para facilitar monitoramento e debug.
 */
@Component
public class BillingScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingScheduler.class);
    
    /**
     * Job para processar cobranças diárias
     * Executa diariamente às 08:00 para processar todas as assinaturas
     * que vencem no dia atual
     */
    @Component
    public static class DailyBillingJob implements Job {
        
        private final ProcessBillingUseCase processBillingUseCase;
        
        public DailyBillingJob(ProcessBillingUseCase processBillingUseCase) {
            this.processBillingUseCase = processBillingUseCase;
        }
        
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("=== INICIANDO JOB DE COBRANÇA DIÁRIA ===");
            
            try {
                ProcessBillingResult result = processBillingUseCase.processScheduledBilling();
                
                logger.info("Job de cobrança diária concluído: success={}, message={}, successCount={}, failureCount={}", 
                    result.isSuccess(), result.message(), result.successCount(), result.failureCount());
                
                // Se houve muitas falhas, logar como warning
                if (result.failureCount() > result.successCount()) {
                    logger.warn("Alto número de falhas na cobrança diária: {} falhas vs {} sucessos", 
                        result.failureCount(), result.successCount());
                }
                
            } catch (Exception e) {
                logger.error("Erro grave no job de cobrança diária: {}", e.getMessage(), e);
                throw new JobExecutionException("Falha na execução do job de cobrança diária", e);
            }
            
            logger.info("=== JOB DE COBRANÇA DIÁRIA FINALIZADO ===");
        }
    }
    
    /**
     * Job para retry de pagamentos falhados
     * Executa a cada 6 horas para tentar reprocessar pagamentos
     * de assinaturas em status PAST_DUE que ainda estão no período de graça
     */
    @Component
    public static class RetryFailedPaymentsJob implements Job {
        
        private final ProcessBillingUseCase processBillingUseCase;
        
        public RetryFailedPaymentsJob(ProcessBillingUseCase processBillingUseCase) {
            this.processBillingUseCase = processBillingUseCase;
        }
        
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("=== INICIANDO JOB DE RETRY DE PAGAMENTOS ===");
            
            try {
                ProcessBillingResult result = processBillingUseCase.processFailedBillingRetries();
                
                logger.info("Job de retry concluído: success={}, message={}, successCount={}, failureCount={}", 
                    result.isSuccess(), result.message(), result.successCount(), result.failureCount());
                
                // Se conseguiu recuperar alguns pagamentos, destacar
                if (result.successCount() > 0) {
                    logger.info("Recuperados {} pagamentos em retry - receita preservada!", result.successCount());
                }
                
            } catch (Exception e) {
                logger.error("Erro grave no job de retry de pagamentos: {}", e.getMessage(), e);
                throw new JobExecutionException("Falha na execução do job de retry", e);
            }
            
            logger.info("=== JOB DE RETRY DE PAGAMENTOS FINALIZADO ===");
        }
    }
    
    /**
     * Job para cancelar assinaturas com período de graça expirado
     * Executa diariamente às 23:00 para cancelar automaticamente
     * assinaturas que ultrapassaram o período de graça sem pagamento
     */
    @Component  
    public static class ExpiredGracePeriodJob implements Job {
        
        private final ProcessBillingUseCase processBillingUseCase;
        
        public ExpiredGracePeriodJob(ProcessBillingUseCase processBillingUseCase) {
            this.processBillingUseCase = processBillingUseCase;
        }
        
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("=== INICIANDO JOB DE CANCELAMENTO POR PERÍODO DE GRAÇA EXPIRADO ===");
            
            try {
                ProcessBillingResult result = processBillingUseCase.processExpiredGracePeriods();
                
                logger.info("Job de cancelamento concluído: success={}, message={}, cancelledCount={}", 
                    result.isSuccess(), result.message(), result.successCount());
                
                // Se cancelou assinaturas, alertar para investigação
                if (result.successCount() > 0) {
                    logger.warn("Canceladas {} assinaturas por período de graça expirado - investigar motivos", 
                        result.successCount());
                }
                
            } catch (Exception e) {
                logger.error("Erro grave no job de cancelamento por graça expirada: {}", e.getMessage(), e);
                throw new JobExecutionException("Falha na execução do job de cancelamento", e);
            }
            
            logger.info("=== JOB DE CANCELAMENTO POR PERÍODO DE GRAÇA EXPIRADO FINALIZADO ===");
        }
    }
    
    /**
     * Utilitários do scheduler

     * Método para forçar execução manual dos jobs (para testes/admin)
     */
    public static class ManualJobTrigger {
        
        private static final Logger logger = LoggerFactory.getLogger(ManualJobTrigger.class);
        
        private final ProcessBillingUseCase processBillingUseCase;
        
        public ManualJobTrigger(ProcessBillingUseCase processBillingUseCase) {
            this.processBillingUseCase = processBillingUseCase;
        }
        
        /**
         * Executa cobrança manual para fins administrativos
         */
        public ProcessBillingResult triggerManualBilling() {
            logger.info("Executando cobrança manual solicitada por administrador");
            
            try {
                return processBillingUseCase.processScheduledBilling();
            } catch (Exception e) {
                logger.error("Erro na execução manual de cobrança: {}", e.getMessage(), e);
                return ProcessBillingResult.failure("Erro na execução manual: " + e.getMessage());
            }
        }
        
        /**
         * Executa retry manual de pagamentos falhados
         */
        public ProcessBillingResult triggerManualRetry() {
            logger.info("Executando retry manual solicitado por administrador");
            
            try {
                return processBillingUseCase.processFailedBillingRetries();
            } catch (Exception e) {
                logger.error("Erro na execução manual de retry: {}", e.getMessage(), e);
                return ProcessBillingResult.failure("Erro no retry manual: " + e.getMessage());
            }
        }
        
        /**
         * Executa cancelamento manual por período expirado
         */
        public ProcessBillingResult triggerManualGraceExpiry() {
            logger.info("Executando cancelamento manual por período expirado");
            
            try {
                return processBillingUseCase.processExpiredGracePeriods();
            } catch (Exception e) {
                logger.error("Erro na execução manual de cancelamento: {}", e.getMessage(), e);
                return ProcessBillingResult.failure("Erro no cancelamento manual: " + e.getMessage());
            }
        }
    }
}