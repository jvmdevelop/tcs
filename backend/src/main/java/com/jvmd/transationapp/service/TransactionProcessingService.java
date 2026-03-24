package com.jvmd.transationapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.model.EStatus;
import com.jvmd.transationapp.model.Transactions;
import com.jvmd.transationapp.repository.TransactionRepository;
import com.jvmd.transationapp.service.rules.RuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionProcessingService {
    private final TransactionRepository transactionRepository;
    private final RuleEngine ruleEngine;
    private final NotificationService notificationService;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processTransaction(UUID transactionId, String correlationId) {
        MDC.put("correlationId", correlationId);
        MDC.put("component", "transaction-processor");
        long startTime = System.currentTimeMillis();
        try {
            log.info("Starting transaction processing: transactionId={}", transactionId);
            Transactions transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
            addProcessingStep(transaction, "PROCESSING_STARTED", "Transaction processing started");
            RuleEngine.RuleEvaluationResult result = ruleEngine.evaluateTransaction(transaction);
            if (result.isAlerted()) {
                transaction.setStatus(EStatus.ALERTED);
                transaction.setAlertReasons(objectMapper.writeValueAsString(result.getAlertReasons()));
                addProcessingStep(transaction, "ALERT_TRIGGERED",
                        String.format("Alert triggered by %d rules", result.getTriggeredRules().size()));
                log.warn("Transaction alerted: transactionId={}, severity={}, rules={}",
                        transactionId, result.getMaxSeverity(), result.getTriggeredRules().size());
                notificationService.sendAlertNotifications(transaction, result);
                metricsService.recordAlert(result.getMaxSeverity());
            } else {
                transaction.setStatus(EStatus.PROCESSED);
                addProcessingStep(transaction, "PROCESSING_COMPLETED", "No alerts triggered");
                log.info("Transaction processed successfully: transactionId={}", transactionId);
                metricsService.recordProcessed();
            }
            if (result.getMlScore() != null) {
                transaction.setMlScore(result.getMlScore());
            }
            transactionRepository.save(transaction);
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordProcessingTime(duration);
            log.info("Transaction processing completed: transactionId={}, status={}, duration={}ms",
                    transactionId, transaction.getStatus(), duration);
        } catch (Exception e) {
            log.error("Error processing transaction: transactionId={}", transactionId, e);
            metricsService.recordError();
            try {
                Transactions transaction = transactionRepository.findById(transactionId).orElse(null);
                if (transaction != null) {
                    addProcessingStep(transaction, "PROCESSING_ERROR", "Error: " + e.getMessage());
                    transactionRepository.save(transaction);
                }
            } catch (Exception ex) {
                log.error("Failed to update transaction with error", ex);
            }
            throw new RuntimeException("Failed to process transaction", e);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("component");
        }
    }

    private void addProcessingStep(Transactions transaction, String step, String details) {
        try {
            List<Map<String, Object>> history = new ArrayList<>();
            if (transaction.getProcessingHistory() != null && !transaction.getProcessingHistory().isEmpty()) {
                history = objectMapper.readValue(
                        transaction.getProcessingHistory(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );
            }
            Map<String, Object> stepData = new HashMap<>();
            stepData.put("timestamp", LocalDateTime.now().toString());
            stepData.put("step", step);
            stepData.put("details", details);
            history.add(stepData);
            transaction.setProcessingHistory(objectMapper.writeValueAsString(history));
        } catch (Exception e) {
            log.error("Error adding processing step", e);
        }
    }
}
