package com.jvmd.transationapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.model.*;
import com.jvmd.transationapp.repository.NotificationConfigRepository;
import com.jvmd.transationapp.repository.NotificationLogRepository;
import com.jvmd.transationapp.service.notification.EmailNotificationSender;
import com.jvmd.transationapp.service.notification.TelegramNotificationSender;
import com.jvmd.transationapp.service.notification.WebhookNotificationSender;
import com.jvmd.transationapp.service.rules.RuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationConfigRepository configRepository;
    private final NotificationLogRepository logRepository;
    private final EmailNotificationSender emailSender;
    private final TelegramNotificationSender telegramSender;
    private final WebhookNotificationSender webhookSender;
    private final ObjectMapper objectMapper;

    /**
     * Отправить уведомления об алерте
     */
    public void sendAlertNotifications(Transactions transaction, RuleEngine.RuleEvaluationResult result) {
        String correlationId = transaction.getCorrelationId();
        MDC.put("correlationId", correlationId);
        MDC.put("component", "notification");

        try {
            log.info("Sending alert notifications: transactionId={}, severity={}", 
                transaction.getId(), result.getMaxSeverity());

            List<NotificationConfig> configs = configRepository.findByEnabledTrue();

            for (NotificationConfig config : configs) {

                if (result.getMaxSeverity() < config.getMinSeverity()) {
                    log.debug("Skipping notification channel {} due to severity filter", config.getChannel());
                    continue;
                }

                sendNotification(config, transaction, result);
            }

        } catch (Exception e) {
            log.error("Error sending notifications", e);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("component");
        }
    }

    private void sendNotification(NotificationConfig config, Transactions transaction, RuleEngine.RuleEvaluationResult result) {
        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setTransactionId(transaction.getId());
        notificationLog.setCorrelationId(transaction.getCorrelationId());
        notificationLog.setChannel(config.getChannel());
        notificationLog.setRetryCount(0);

        try {
            String message = buildMessage(config, transaction, result);
            notificationLog.setMessage(message);

            boolean success = switch (config.getChannel()) {
                case EMAIL -> emailSender.send(config, message, transaction);
                case TELEGRAM -> telegramSender.send(config, message, transaction);
                case WEBHOOK -> webhookSender.send(config, message, transaction);
            };

            notificationLog.setStatus(success ? "SUCCESS" : "FAILED");
            
            if (success) {
                log.info("Notification sent successfully: channel={}, transactionId={}", 
                    config.getChannel(), transaction.getId());
            } else {
                log.warn("Notification failed: channel={}, transactionId={}", 
                    config.getChannel(), transaction.getId());
            }

        } catch (Exception e) {
            log.error("Error sending notification via {}", config.getChannel(), e);
            notificationLog.setStatus("FAILED");
            notificationLog.setError(e.getMessage());
        }

        logRepository.save(notificationLog);
    }

    private String buildMessage(NotificationConfig config, Transactions transaction, RuleEngine.RuleEvaluationResult result) {
        try {
            String template = config.getMessageTemplate();
            if (template == null || template.isEmpty()) {
                template = getDefaultTemplate();
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("transactionId", transaction.getId().toString());
            variables.put("correlationId", transaction.getCorrelationId());
            variables.put("amount", transaction.getAmount().toString());
            variables.put("from", transaction.getFrom());
            variables.put("to", transaction.getTo());
            variables.put("type", transaction.getType());
            variables.put("timestamp", transaction.getTimestamp().toString());
            variables.put("severity", result.getMaxSeverity());
            variables.put("mlScore", result.getMlScore() != null ? result.getMlScore().toString() : "N/A");
            variables.put("triggeredRules", result.getTriggeredRules().size());
            variables.put("reasons", String.join(", ", result.getAlertReasons()));
            variables.put("detailsUrl", "http://localhost:8080/admin/transactions/" + transaction.getId());

            return replacePlaceholders(template, variables);

        } catch (Exception e) {
            log.error("Error building notification message", e);
            return "Alert: Transaction " + transaction.getId() + " triggered fraud detection rules.";
        }
    }

    private String replacePlaceholders(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    private String getDefaultTemplate() {
        return """
            🚨 FRAUD ALERT
            
            Transaction ID: {{transactionId}}
            Correlation ID: {{correlationId}}
            Amount: {{amount}}
            From: {{from}}
            To: {{to}}
            Type: {{type}}
            Time: {{timestamp}}
            
            Severity: {{severity}}/5
            ML Score: {{mlScore}}
            Triggered Rules: {{triggeredRules}}
            
            Reasons: {{reasons}}
            
            Details: {{detailsUrl}}
            """;
    }
}
