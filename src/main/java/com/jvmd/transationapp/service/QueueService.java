package com.jvmd.transationapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueService {
    private static final String QUEUE_NAME = "transaction:queue";
    private static final String PROCESSING_SET = "transaction:processing";
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.queue.retry-attempts:3}")
    private int maxRetryAttempts;

    public void enqueue(UUID transactionId, String correlationId) {
        try {
            MDC.put("correlationId", correlationId);
            QueueMessage message = new QueueMessage();
            message.setTransactionId(transactionId);
            message.setCorrelationId(correlationId);
            message.setEnqueuedAt(System.currentTimeMillis());
            message.setRetryCount(0);
            redisTemplate.opsForList().rightPush(QUEUE_NAME, message);
            log.info("Transaction enqueued: transactionId={}, correlationId={}",
                    transactionId, correlationId);
        } catch (Exception e) {
            log.error("Failed to enqueue transaction: transactionId={}, correlationId={}",
                    transactionId, correlationId, e);
            throw new RuntimeException("Failed to enqueue transaction", e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    public QueueMessage dequeue() {
        try {
            Object message = redisTemplate.opsForList().leftPop(QUEUE_NAME);
            if (message != null) {
                QueueMessage queueMessage = objectMapper.convertValue(message, QueueMessage.class);
                redisTemplate.opsForSet().add(PROCESSING_SET, queueMessage.getTransactionId().toString());
                redisTemplate.expire(PROCESSING_SET, 1, TimeUnit.HOURS);
                return queueMessage;
            }
        } catch (Exception e) {
            log.error("Failed to dequeue transaction", e);
        }
        return null;
    }

    public void markAsProcessed(UUID transactionId) {
        redisTemplate.opsForSet().remove(PROCESSING_SET, transactionId.toString());
    }

    public void requeueForRetry(QueueMessage message) {
        if (message.getRetryCount() < maxRetryAttempts) {
            message.setRetryCount(message.getRetryCount() + 1);
            redisTemplate.opsForList().rightPush(QUEUE_NAME, message);
            log.warn("Transaction requeued for retry: transactionId={}, retryCount={}",
                    message.getTransactionId(), message.getRetryCount());
        } else {
            log.error("Transaction exceeded max retry attempts: transactionId={}",
                    message.getTransactionId());
            markAsProcessed(message.getTransactionId());
        }
    }

    public Long getQueueSize() {
        Long size = redisTemplate.opsForList().size(QUEUE_NAME);
        return size != null ? size : 0L;
    }

    public boolean isProcessing(UUID transactionId) {
        Boolean isMember = redisTemplate.opsForSet().isMember(PROCESSING_SET, transactionId.toString());
        return isMember != null && isMember;
    }

    @lombok.Data
    public static class QueueMessage {
        private UUID transactionId;
        private String correlationId;
        private Long enqueuedAt;
        private Integer retryCount;
    }
}
