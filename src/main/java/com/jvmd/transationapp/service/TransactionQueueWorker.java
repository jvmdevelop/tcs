package com.jvmd.transationapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionQueueWorker {
    private final QueueService queueService;
    private final TransactionProcessingService processingService;
    @Value("${app.queue.worker-threads:5}")
    private int workerThreads;
    private ExecutorService executorService;
    private volatile boolean running = false;

    @EventListener(ApplicationReadyEvent.class)
    public void startWorkers() {
        log.info("Starting {} queue worker threads", workerThreads);
        executorService = Executors.newFixedThreadPool(workerThreads);
        running = true;
        for (int i = 0; i < workerThreads; i++) {
            final int workerId = i;
            executorService.submit(() -> processQueue(workerId));
        }
    }

    private void processQueue(int workerId) {
        log.info("Queue worker {} started", workerId);
        while (running) {
            try {
                QueueService.QueueMessage message = queueService.dequeue();
                if (message != null) {
                    try {
                        log.debug("Worker {} processing transaction: {}",
                                workerId, message.getTransactionId());
                        processingService.processTransaction(
                                message.getTransactionId(),
                                message.getCorrelationId()
                        );
                        queueService.markAsProcessed(message.getTransactionId());
                    } catch (Exception e) {
                        log.error("Worker {} failed to process transaction: {}",
                                workerId, message.getTransactionId(), e);
                        queueService.requeueForRetry(message);
                    }
                } else {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Worker {} interrupted", workerId);
                break;
            } catch (Exception e) {
                log.error("Worker {} encountered error", workerId, e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("Queue worker {} stopped", workerId);
    }

    @PreDestroy
    public void stopWorkers() {
        log.info("Stopping queue workers");
        running = false;
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
