package com.jvmd.transationapp.service.notification;

import com.jvmd.transationapp.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConfigValidator implements ApplicationRunner {
    private final NotificationConfigRepository repository;
    
    @Override
    public void run(ApplicationArguments args) {
        repository.findAll().forEach(config -> {
            if (!isValidTemplate(config.getMessageTemplate())) {
                throw new IllegalStateException("Invalid template for config: " + config.getId());
            }
        });
    }
}