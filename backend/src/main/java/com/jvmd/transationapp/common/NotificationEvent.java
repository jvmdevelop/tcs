package com.jvmd.transationapp.common;

import com.jvmd.transationapp.model.NotificationConfig;
import com.jvmd.transationapp.model.Transactions;
import com.jvmd.transationapp.service.rules.RuleEngine;
import lombok.Builder;
import lombok.Value;
import org.springframework.context.ApplicationEvent;

@Builder
@Value
public class NotificationEvent extends ApplicationEvent {
    Transactions transaction;
    RuleEngine.RuleEvaluationResult result;
    NotificationConfig config;
}