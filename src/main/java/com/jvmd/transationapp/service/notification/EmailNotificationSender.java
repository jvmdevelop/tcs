package com.jvmd.transationapp.service.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.model.NotificationConfig;
import com.jvmd.transationapp.model.Transactions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationSender {
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    public boolean send(NotificationConfig config, String message, Transactions transaction) {
        try {
            Map<String, Object> emailConfig = objectMapper.readValue(
                    config.getConfiguration(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            String to = (String) emailConfig.get("to");
            String subject = emailConfig.getOrDefault("subject", "Fraud Alert").toString();
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject + " - " + transaction.getCorrelationId());
            mailMessage.setText(message);
            mailSender.send(mailMessage);
            log.info("Email sent successfully to {}", to);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email notification", e);
            return false;
        }
    }
}
