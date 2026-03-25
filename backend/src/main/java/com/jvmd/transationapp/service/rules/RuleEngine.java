package com.jvmd.transationapp.service.rules;

import com.jvmd.transationapp.common.RuleEvaluationResult;
import com.jvmd.transationapp.model.Rule;
import com.jvmd.transationapp.model.Transactions;
import com.jvmd.transationapp.repository.RuleRepository;
import com.jvmd.transationapp.service.rules.evaluator.Evaluators;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEngine {
    private final RuleRepository ruleRepository;
    private final Evaluators evaluators;
    private List<Rule> activeRules = new ArrayList<>();


    @PostConstruct
    public void loadRules() {
        try {
            activeRules = ruleRepository.findByEnabledTrueOrderByPriorityAsc();
            log.info("Loaded {} active rules", activeRules.size());
        } catch (Exception e) {
            log.error("Failed to load rules", e);
            activeRules = new ArrayList<>();
        }
    }

    public void reloadRules() {
        loadRules();
        log.info("Rules reloaded: {} active rules", activeRules.size());
    }

    public RuleEvaluationResult evaluateTransaction(Transactions transaction) {
        String correlationId = transaction.getCorrelationId();
        MDC.put("correlationId", correlationId);
        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setTransactionId(transaction.getId());
        result.setCorrelationId(correlationId);
        result.setTriggeredRules(new ArrayList<>());
        result.setAlertReasons(new ArrayList<>());
        try {
            log.debug("Evaluating {} rules for transaction {}", activeRules.size(), transaction.getId());
            for (Rule rule : activeRules) {
                try {
                    long startTime = System.currentTimeMillis();
                    boolean triggered = evaluateRule(rule, transaction);
                    long executionTime = System.currentTimeMillis() - startTime;
                    log.debug("Rule {} evaluated in {}ms: triggered={}",
                            rule.getName(), executionTime, triggered);
                    if (triggered) {
                        result.getTriggeredRules().add(rule);
                        result.getAlertReasons().add(String.format(
                                "Rule '%s' (type: %s, severity: %d) triggered",
                                rule.getName(), rule.getType(), rule.getSeverity()
                        ));
                        result.setAlerted(true);
                        result.setMaxSeverity(Math.max(result.getMaxSeverity(), rule.getSeverity()));
                        if (rule.getSeverity() >= 4) {
                            log.warn("Critical rule triggered, short-circuiting: rule={}", rule.getName());
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error evaluating rule {}: {}", rule.getName(), e.getMessage(), e);
                }
            }
            log.info("Transaction evaluation complete: alerted={}, triggeredRules={}",
                    result.isAlerted(), result.getTriggeredRules().size());
        } finally {
            MDC.remove("correlationId");
        }
        return result;
    }

    private boolean evaluateRule(Rule rule, Transactions transaction) {
        return evaluators.getEvaluator(rule).evaluate(rule, transaction);
    }


}
