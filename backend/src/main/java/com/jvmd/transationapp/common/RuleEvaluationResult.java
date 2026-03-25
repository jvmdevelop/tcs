package com.jvmd.transationapp.common;

import com.jvmd.transationapp.model.Rule;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RuleEvaluationResult {
    private UUID transactionId;
    private String correlationId;
    private boolean alerted = false;
    private List<Rule> triggeredRules;
    private List<String> alertReasons;
    private int maxSeverity = 0;
    private Double mlScore;
}