package com.ecommerce.platform.subscription.infrastructure.config;

import com.ecommerce.platform.subscription.adapter.out.scheduler.BillingScheduler;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Quartz Scheduler
 * Define jobs e triggers para processamento automático de cobranças recorrentes.
 * Implementa estratégia de scheduling robusta para garantir que as cobranças
 * sejam processadas de forma confiável e pontual.
 */
@Configuration
public class QuartzConfig {

    /**
     * Job Details para cobrança diária
     * Processa todas as assinaturas que vencem no dia atual
     */
    @Bean
    public JobDetail dailyBillingJobDetail() {
        return JobBuilder.newJob(BillingScheduler.DailyBillingJob.class)
            .withIdentity("dailyBillingJob", "billing")
            .withDescription("Processa cobranças diárias de assinaturas")
            .storeDurably() // Job persiste mesmo sem trigger ativo
            .requestRecovery() // Recupera execução se aplicação for reiniciada
            .build();
    }

    /**
     * Trigger para cobrança diária
     * Executa todos os dias às 08:00 (horário do servidor)
     */
    @Bean
    public Trigger dailyBillingTrigger() {
        return TriggerBuilder.newTrigger()
            .forJob(dailyBillingJobDetail())
            .withIdentity("dailyBillingTrigger", "billing")
            .withDescription("Trigger diário para cobrança às 08:00")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 0 8 * * ?") // 08:00 todos os dias
                    .withMisfireHandlingInstructionDoNothing() // Não executa se perdeu horário
            )
            .startNow()
            .build();
    }

    /**
     * Job Details para retry de pagamentos falhados
     * Tenta reprocessar pagamentos de assinaturas em PAST_DUE
     */
    @Bean
    public JobDetail retryFailedPaymentsJobDetail() {
        return JobBuilder.newJob(BillingScheduler.RetryFailedPaymentsJob.class)
            .withIdentity("retryFailedPaymentsJob", "billing")
            .withDescription("Retry de pagamentos falhados")
            .storeDurably()
            .requestRecovery()
            .build();
    }

    /**
     * Trigger para retry de pagamentos
     * Executa a cada 6 horas para dar múltiplas chances de recuperação
     */
    @Bean
    public Trigger retryFailedPaymentsTrigger() {
        return TriggerBuilder.newTrigger()
            .forJob(retryFailedPaymentsJobDetail())
            .withIdentity("retryFailedPaymentsTrigger", "billing")
            .withDescription("Trigger a cada 6 horas para retry de pagamentos")
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInHours(6) // A cada 6 horas
                    .repeatForever()
                    .withMisfireHandlingInstructionNextWithRemainingCount()
            )
            .startNow()
            .build();
    }

    /**
     * Job Details para cancelamento por período de graça expirado
     * Cancela assinaturas que ultrapassaram período de graça
     */
    @Bean
    public JobDetail expiredGracePeriodJobDetail() {
        return JobBuilder.newJob(BillingScheduler.ExpiredGracePeriodJob.class)
            .withIdentity("expiredGracePeriodJob", "billing")
            .withDescription("Cancela assinaturas com período de graça expirado")
            .storeDurably()
            .requestRecovery()
            .build();
    }

    /**
     * Trigger para cancelamento por graça expirada
     * Executa diariamente às 23:00 para limpeza
     */
    @Bean
    public Trigger expiredGracePeriodTrigger() {
        return TriggerBuilder.newTrigger()
            .forJob(expiredGracePeriodJobDetail())
            .withIdentity("expiredGracePeriodTrigger", "billing")
            .withDescription("Trigger diário para cancelamento às 23:00")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 0 23 * * ?") // 23:00 todos os dias
                    .withMisfireHandlingInstructionDoNothing()
            )
            .startNow()
            .build();
    }
}