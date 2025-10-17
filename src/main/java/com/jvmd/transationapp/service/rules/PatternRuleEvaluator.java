package com.jvmd.transationapp.service.rules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.model.Rule;
import com.jvmd.transationapp.model.Transactions;
import com.jvmd.transationapp.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class PatternRuleEvaluator {
    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;

    public boolean evaluate(Rule rule, Transactions transaction) {
        try {
            Map<String, Object> config = objectMapper.readValue(
                    rule.getConfiguration(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            String patternType = (String) config.get("type");
            return switch (patternType) {
                case "multiple_small_transactions" -> evaluateMultipleSmallTransactions(config, transaction);
                case "rapid_succession" -> evaluateRapidSuccession(config, transaction);
                default -> {
                    log.warn("Unknown pattern type: {}", patternType);
                    yield false;
                }
            };
        } catch (Exception e) {
            log.error("Error evaluating pattern rule: {}", e.getMessage());
            return false;
        }
    }

    private boolean evaluateMultipleSmallTransactions(Map<String, Object> config, Transactions transaction) {
        try {
            int timeWindowMinutes = (Integer) config.get("timeWindowMinutes");
            int minTransactions = (Integer) config.get("minTransactions");
            double maxAmountPerTransaction = ((Number) config.get("maxAmountPerTransaction")).doubleValue();
            String accountField = (String) config.get("accountField");
            LocalDateTime windowStart = transaction.getTimestamp().minusMinutes(timeWindowMinutes);
            LocalDateTime windowEnd = transaction.getTimestamp();
            List<Transactions> recentTransactions;
            if ("from".equals(accountField)) {
                recentTransactions = transactionRepository.findByFromAndTimestampBetween(
                        transaction.getFrom(), windowStart, windowEnd
                );
            } else {
                recentTransactions = transactionRepository.findByToAndTimestampBetween(
                        transaction.getTo(), windowStart, windowEnd
                );
            }
            long smallTransactionCount = recentTransactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(maxAmountPerTransaction)) <= 0)
                    .count();
            boolean triggered = smallTransactionCount >= minTransactions;
            if (triggered) {
                log.warn("Pattern detected: {} small transactions from {} in {} minutes",
                        smallTransactionCount, accountField.equals("from") ? transaction.getFrom() : transaction.getTo(),
                        timeWindowMinutes);
            }
            return triggered;
        } catch (Exception e) {
            log.error("Error evaluating multiple small transactions pattern: {}", e.getMessage());
            return false;
        }
    }

    private boolean evaluateRapidSuccession(Map<String, Object> config, Transactions transaction) {
        try {
            int timeWindowSeconds = (Integer) config.get("timeWindowSeconds");
            int minTransactions = (Integer) config.get("minTransactions");
            LocalDateTime windowStart = transaction.getTimestamp().minusSeconds(timeWindowSeconds);
            LocalDateTime windowEnd = transaction.getTimestamp();
            List<Transactions> recentTransactions = transactionRepository
                    .findByFromAndTimestampBetween(transaction.getFrom(), windowStart, windowEnd);
            return recentTransactions.size() >= minTransactions;
        } catch (Exception e) {
            log.error("Error evaluating rapid succession pattern: {}", e.getMessage());
            return false;
        }
    }
}
