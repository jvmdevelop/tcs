package com.jvmd.transationapp.service.rules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.model.Rule;
import com.jvmd.transationapp.model.Transactions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class CompositeRuleEvaluator {
    private final ObjectMapper objectMapper;

    public boolean evaluate(Rule rule, Transactions transaction) {
        try {
            Map<String, Object> config = objectMapper.readValue(
                    rule.getConfiguration(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            String operator = (String) config.get("operator");
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.get("conditions");
            return switch (operator) {
                case "AND" -> evaluateAnd(conditions, transaction);
                case "OR" -> evaluateOr(conditions, transaction);
                case "NOT" -> evaluateNot(conditions, transaction);
                default -> {
                    log.warn("Unknown composite operator: {}", operator);
                    yield false;
                }
            };
        } catch (Exception e) {
            log.error("Error evaluating composite rule: {}", e.getMessage());
            return false;
        }
    }

    private boolean evaluateAnd(List<Map<String, Object>> conditions, Transactions transaction) {
        return conditions.stream().allMatch(condition -> evaluateCondition(condition, transaction));
    }

    private boolean evaluateOr(List<Map<String, Object>> conditions, Transactions transaction) {
        return conditions.stream().anyMatch(condition -> evaluateCondition(condition, transaction));
    }

    private boolean evaluateNot(List<Map<String, Object>> conditions, Transactions transaction) {
        return conditions.stream().noneMatch(condition -> evaluateCondition(condition, transaction));
    }

    private boolean evaluateCondition(Map<String, Object> condition, Transactions transaction) {
        String type = (String) condition.get("type");
        return switch (type) {
            case "amount" -> evaluateAmountCondition(condition, transaction);
            case "nighttime" -> evaluateNighttimeCondition(condition, transaction);
            case "type" -> evaluateTypeCondition(condition, transaction);
            case "account" -> evaluateAccountCondition(condition, transaction);
            default -> {
                log.warn("Unknown condition type: {}", type);
                yield false;
            }
        };
    }

    private boolean evaluateAmountCondition(Map<String, Object> condition, Transactions transaction) {
        String operator = (String) condition.get("operator");
        BigDecimal value = new BigDecimal(condition.get("value").toString());
        return switch (operator) {
            case ">" -> transaction.getAmount().compareTo(value) > 0;
            case ">=" -> transaction.getAmount().compareTo(value) >= 0;
            case "<" -> transaction.getAmount().compareTo(value) < 0;
            case "<=" -> transaction.getAmount().compareTo(value) <= 0;
            case "=" -> transaction.getAmount().compareTo(value) == 0;
            default -> false;
        };
    }

    private boolean evaluateNighttimeCondition(Map<String, Object> condition, Transactions transaction) {
        int startHour = (Integer) condition.get("startHour");
        int endHour = (Integer) condition.get("endHour");
        LocalTime transactionTime = transaction.getTimestamp().toLocalTime();
        int hour = transactionTime.getHour();
        if (startHour > endHour) {
            return hour >= startHour || hour < endHour;
        } else {
            return hour >= startHour && hour < endHour;
        }
    }

    private boolean evaluateTypeCondition(Map<String, Object> condition, Transactions transaction) {
        String expectedType = (String) condition.get("value");
        return transaction.getType().equals(expectedType);
    }

    private boolean evaluateAccountCondition(Map<String, Object> condition, Transactions transaction) {
        String field = (String) condition.get("field");
        String pattern = (String) condition.get("pattern");
        String accountValue = "from".equals(field) ? transaction.getFrom() : transaction.getTo();
        return accountValue.matches(pattern);
    }
}
