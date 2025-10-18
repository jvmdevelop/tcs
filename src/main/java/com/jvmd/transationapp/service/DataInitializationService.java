package com.jvmd.transationapp.service;

import com.jvmd.transationapp.config.NotificationSendProperties;
import com.jvmd.transationapp.model.*;
import com.jvmd.transationapp.repository.NotificationConfigRepository;
import com.jvmd.transationapp.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Инициализация начальных данных при запуске приложения
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataInitializationService {

    private final RuleRepository ruleRepository;
    private final NotificationConfigRepository notificationConfigRepository;
    private final NotificationSendProperties notificationSendProperties;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeData() {
        log.info("Initializing default data...");

        createDefaultRulesIfNeeded();
        createDefaultNotificationConfigsIfNeeded();

        log.info("Default data initialization completed");
    }

    private void createDefaultRulesIfNeeded() {
        if (ruleRepository.count() == 0) {
            log.info("Creating default rules...");

            Rule highAmountRule = new Rule();
            highAmountRule.setName("High Amount Transaction");
            highAmountRule.setDescription("Alert on transactions above 100,000");
            highAmountRule.setType(RuleType.THRESHOLD);
            highAmountRule.setConfiguration("{\"field\":\"amount\",\"operator\":\">\",\"value\":100000}");
            highAmountRule.setEnabled(true);
            highAmountRule.setPriority(1);
            highAmountRule.setSeverity(4);
            highAmountRule.setCreatedBy("system");
            ruleRepository.save(highAmountRule);

            Rule patternRule = new Rule();
            patternRule.setName("Multiple Small Transactions");
            patternRule.setDescription("Detect series of small transactions in short time window");
            patternRule.setType(RuleType.PATTERN);
            patternRule.setConfiguration("{\"type\":\"multiple_small_transactions\",\"timeWindowMinutes\":10,\"minTransactions\":5,\"maxAmountPerTransaction\":1000,\"accountField\":\"from\"}");
            patternRule.setEnabled(true);
            patternRule.setPriority(2);
            patternRule.setSeverity(3);
            patternRule.setCreatedBy("system");
            ruleRepository.save(patternRule);


            Rule compositeRule = new Rule();
            compositeRule.setName("Large Nighttime Transaction");
            compositeRule.setDescription("Large transactions during nighttime hours");
            compositeRule.setType(RuleType.COMPOSITE);
            compositeRule.setConfiguration("{\"operator\":\"AND\",\"conditions\":[{\"type\":\"amount\",\"operator\":\">\",\"value\":50000},{\"type\":\"nighttime\",\"startHour\":22,\"endHour\":6}]}");
            compositeRule.setEnabled(true);
            compositeRule.setPriority(3);
            compositeRule.setSeverity(5);
            compositeRule.setCreatedBy("system");
            ruleRepository.save(compositeRule);


            Rule mlRule = new Rule();
            mlRule.setName("ML Fraud Detection");
            mlRule.setDescription("Machine learning based fraud detection");
            mlRule.setType(RuleType.ML_RULE);
            mlRule.setConfiguration("{\"modelPath\":\"models/fraud_model.pt\",\"threshold\":0.7,\"modelVersion\":\"1.0\"}");
            mlRule.setEnabled(false);
            mlRule.setPriority(4);
            mlRule.setSeverity(4);
            mlRule.setCreatedBy("system");
            ruleRepository.save(mlRule);

            log.info("Created {} default rules", 4);
        }
    }

    private void createDefaultNotificationConfigsIfNeeded() {
        if (notificationConfigRepository.count() == 0) {
            log.info("Creating default notification configurations...");


            NotificationConfig emailConfig = new NotificationConfig();
            emailConfig.setChannel(NotificationChannel.EMAIL);
            emailConfig.setEnabled(false);
            emailConfig.setMinSeverity(3);
            emailConfig.setConfiguration("{\"to\":\"" + notificationSendProperties.getMail() +"\",\"subject\":\"Fraud Alert\"}");
            emailConfig.setMessageTemplate("🚨 FRAUD ALERT\\n\\nTransaction ID: {{transactionId}}\\nCorrelation ID: {{correlationId}}\\nAmount: {{amount}}\\nFrom: {{from}}\\nTo: {{to}}\\nType: {{type}}\\nTime: {{timestamp}}\\n\\nSeverity: {{severity}}/5\\nML Score: {{mlScore}}\\nTriggered Rules: {{triggeredRules}}\\n\\nReasons: {{reasons}}\\n\\nDetails: {{detailsUrl}}");
            notificationConfigRepository.save(emailConfig);


            NotificationConfig telegramConfig = new NotificationConfig();
            telegramConfig.setChannel(NotificationChannel.TELEGRAM);
            telegramConfig.setEnabled(false);
            telegramConfig.setMinSeverity(3);
            telegramConfig.setConfiguration("{\"chatId\":\"%s\"}".formatted(notificationSendProperties.getTgId()));
            telegramConfig.setMessageTemplate("🚨 <b>FRAUD ALERT</b>\\n\\n<b>Transaction:</b> {{transactionId}}\\n<b>Amount:</b> {{amount}}\\n<b>From:</b> {{from}}\\n<b>To:</b> {{to}}\\n<b>Severity:</b> {{severity}}/5\\n<b>ML Score:</b> {{mlScore}}\\n\\n<b>Reasons:</b> {{reasons}}\\n\\n<a href='{{detailsUrl}}'>View Details</a>");
            notificationConfigRepository.save(telegramConfig);


            NotificationConfig webhookConfig = new NotificationConfig();
            webhookConfig.setChannel(NotificationChannel.WEBHOOK);
            webhookConfig.setEnabled(false);
            webhookConfig.setMinSeverity(3);
            webhookConfig.setConfiguration("{\"url\":\"https://your-webhook-url.com/alerts\",\"method\":\"POST\"}");
            webhookConfig.setMessageTemplate("Alert");
            notificationConfigRepository.save(webhookConfig);

            log.info("Created {} default notification configurations", 3);
        }
    }
}
