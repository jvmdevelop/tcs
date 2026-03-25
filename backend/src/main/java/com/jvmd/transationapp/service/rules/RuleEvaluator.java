package com.jvmd.transationapp.service.rules;

import com.jvmd.transationapp.model.Rule;
import com.jvmd.transationapp.model.Transactions;

public interface RuleEvaluator {
    boolean evaluate(Rule rule, Transactions transaction);
}
