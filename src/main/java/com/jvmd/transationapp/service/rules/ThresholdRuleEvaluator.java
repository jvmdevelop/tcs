package com.jvmd.transationapp.service.rules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.model.Rule;
import com.jvmd.transationapp.model.Transactions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ThresholdRuleEvaluator {
    private final ObjectMapper objectMapper;

    public boolean evaluate(Rule rule, Transactions transaction) {
        try {
            Map<String, Object> config = objectMapper.readValue(
                    rule.getConfiguration(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            String field = (String) config.get("field");
            String operator = (String) config.get("operator");
            Object thresholdValue = config.get("value");
            BigDecimal actualValue = getFieldValue(transaction, field);
            BigDecimal threshold = new BigDecimal(thresholdValue.toString());
            return switch (operator) {
                case ">" -> actualValue.compareTo(threshold) > 0;
                case ">=" -> actualValue.compareTo(threshold) >= 0;
                case "<" -> actualValue.compareTo(threshold) < 0;
                case "<=" -> actualValue.compareTo(threshold) <= 0;
                case "=" -> actualValue.compareTo(threshold) == 0;
                case "!=" -> actualValue.compareTo(threshold) != 0;
                default -> {
                    log.warn("Unknown operator: {}", operator);
                    yield false;
                }
            };
        } catch (Exception e) {
            log.error("Error evaluating threshold rule: {}", e.getMessage());
            return false;
        }
    }

    private BigDecimal getFieldValue(Transactions transaction, String field) {
        return switch (field) {
            case "amount" -> transaction.getAmount();
            case "mlScore" -> transaction.getMlScore() != null
                    ? BigDecimal.valueOf(transaction.getMlScore())
                    : BigDecimal.ZERO;
            default -> BigDecimal.ZERO;
        };
    }
}
