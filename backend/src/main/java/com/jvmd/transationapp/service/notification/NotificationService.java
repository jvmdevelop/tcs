package com.jvmd.transationapp.service.notification;

import com.jvmd.transationapp.common.NotificationEvent;
import com.jvmd.transationapp.model.Transactions;
import com.jvmd.transationapp.repository.NotificationConfigRepository;
import com.jvmd.transationapp.service.rules.RuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationConfigRepository configRepository;
    private final ApplicationEventPublisher publisher;

    public void sendAlertNotifications(Transactions transaction, RuleEngine.RuleEvaluationResult result) {
        String correlationId = transaction.getCorrelationId();
        MDC.put("correlationId", correlationId);
        MDC.put("component", "notification");

        try {
            log.info("Triggering alerts: tx={}, severity={}",
                    transaction.getId(), result.getMaxSeverity());
            configRepository.findByEnabledTrue().forEach(config ->
                    publisher.publishEvent(NotificationEvent.builder()
                            .transaction(transaction)
                            .result(result)
                            .config(config)
                            .build())
            );

        } catch (Exception e) {
            log.error("Failed to publish notification events", e);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("component");
        }
    }
}