package com.jvmd.transationapp.service.rules.evaluator;

import com.jvmd.transationapp.model.Rule;
import com.jvmd.transationapp.service.rules.evaluator.impl.CompositeRuleEvaluator;
import com.jvmd.transationapp.service.rules.evaluator.impl.MLRuleEvaluator;
import com.jvmd.transationapp.service.rules.evaluator.impl.PatternRuleEvaluator;
import com.jvmd.transationapp.service.rules.evaluator.impl.ThresholdRuleEvaluator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class Evaluators {
    private final ThresholdRuleEvaluator thresholdEvaluator;
    private final PatternRuleEvaluator patternEvaluator;
    private final CompositeRuleEvaluator compositeEvaluator;
    private final MLRuleEvaluator mlRuleEvaluator;


    public RuleEvaluator getEvaluator(Rule rule) {
        return switch (rule.getType()) {
            case THRESHOLD -> thresholdEvaluator;
            case PATTERN -> patternEvaluator;
            case COMPOSITE -> compositeEvaluator;
            case ML_RULE -> mlRuleEvaluator;
        };
    }


}
