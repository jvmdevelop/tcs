package com.jvmd.transationapp.service.rules.evaluator;

import com.jvmd.transationapp.model.Rule;
import com.jvmd.transationapp.model.Transactions;

public interface RuleEvaluator {
    boolean evaluate(Rule rule, Transactions transaction);
}
