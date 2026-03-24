package com.jvmd.transationapp.service;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {
    private final MeterRegistry meterRegistry;
    public void recordProcessed() {
        Counter.builder("transactions.processed")
            .description("Number of processed transactions")
            .register(meterRegistry)
            .increment();
    }
    public void recordAlert(int severity) {
        Counter.builder("transactions.alerted")
            .description("Number of alerted transactions")
            .tag("severity", String.valueOf(severity))
            .register(meterRegistry)
            .increment();
    }
    public void recordReviewed() {
        Counter.builder("transactions.reviewed")
            .description("Number of reviewed transactions")
            .register(meterRegistry)
            .increment();
    }
    public void recordError() {
        Counter.builder("transactions.errors")
            .description("Number of transaction processing errors")
            .register(meterRegistry)
            .increment();
    }
    public void recordProcessingTime(long durationMs) {
        Timer.builder("transactions.processing.time")
            .description("Transaction processing time")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    public void recordRuleExecutionTime(String ruleName, long durationMs) {
        Timer.builder("rules.execution.time")
            .description("Rule execution time")
            .tag("rule", ruleName)
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    public void recordNotificationSent(String channel, boolean success) {
        Counter.builder("notifications.sent")
            .description("Number of notifications sent")
            .tag("channel", channel)
            .tag("status", success ? "success" : "failed")
            .register(meterRegistry)
            .increment();
    }
}
