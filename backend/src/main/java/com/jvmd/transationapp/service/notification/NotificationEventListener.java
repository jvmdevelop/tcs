package com.jvmd.transationapp.service.notification;

import com.jvmd.transationapp.common.NotificationEvent;
import com.jvmd.transationapp.common.TransientException;
import com.jvmd.transationapp.model.NotificationLog;
import com.jvmd.transationapp.service.notification.sender.Senders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final Senders senders;
    private final NotificationTemplateService templateService;
    private final NotificationLogService logService;

    @Async("notificationExecutor")
    @EventListener(NotificationEvent.class)
    @Retryable(
            value = {TransientException.class},
            maxAttemptsExpression = "#{@notificationProperties.maxRetries}",
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handle(NotificationEvent event) {
        var config = event.getConfig();
        var tx = event.getTransaction();
        var result = event.getResult();

        if (result.getMaxSeverity() < config.getMinSeverity()) {
            log.debug("Skipped {} (severity {} < {})", config.getChannel(),
                    result.getMaxSeverity(), config.getMinSeverity());
            return;
        }

        NotificationLog logEntry = new NotificationLog();
        logEntry.setTransactionId(tx.getId());
        logEntry.setCorrelationId(tx.getCorrelationId());
        logEntry.setChannel(config.getChannel());
        logEntry.setRetryCount(0);

        try {
            String message = templateService.build(config, tx, result);
            logEntry.setMessage(message);


            boolean success = senders.getSender(config.getChannel())
                    .send(config, message, tx);

            logEntry.setStatus(success ? "SUCCESS" : "FAILED");
            if (!success) {
                throw new TransientException("Sender returned false");
            }

            log.info("Sent via {}: tx={}", config.getChannel(), tx.getId());

        } catch (TransientException e) {
            log.warn("Transient error for {}, retrying...", config.getChannel());
            throw e;
        } catch (Exception e) {
            log.error("Failed to send via {}", config.getChannel(), e);
            logEntry.setStatus("FAILED");
            logEntry.setError(e.getMessage());
        } finally {
            logService.saveAsync(logEntry);
        }
    }
}