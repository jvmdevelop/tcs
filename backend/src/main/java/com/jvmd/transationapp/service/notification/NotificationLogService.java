package com.jvmd.transationapp.service.notification;

import com.jvmd.transationapp.model.NotificationLog;
import com.jvmd.transationapp.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationLogService {

    private final NotificationLogRepository repository;

    @Async("notificationExecutor")
    public void saveAsync(NotificationLog log) {
        try {
            repository.save(log);
        } catch (Exception e) {
            log.error("Failed to save notification log", e);
        }
    }
}