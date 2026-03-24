package com.jvmd.transationapp.service.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.model.NotificationConfig;
import com.jvmd.transationapp.model.Transactions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookNotificationSender {
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    public boolean send(NotificationConfig config, String message, Transactions transaction) {
        try {
            Map<String, Object> webhookConfig = objectMapper.readValue(
                    config.getConfiguration(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            String url = (String) webhookConfig.get("url");
            String method = webhookConfig.getOrDefault("method", "POST").toString();

            Map<String, Object> payload = new HashMap<>();
            payload.put("transactionId", transaction.getId().toString());
            payload.put("correlationId", transaction.getCorrelationId());
            payload.put("amount", transaction.getAmount().toString());
            payload.put("from", transaction.getFrom());
            payload.put("to", transaction.getTo());
            payload.put("type", transaction.getType());
            payload.put("timestamp", transaction.getTimestamp().toString());
            payload.put("status", transaction.getStatus().toString());
            payload.put("message", message);

            WebClient webClient = webClientBuilder.build();
            if ("POST".equalsIgnoreCase(method)) {
                webClient.post()
                        .uri(url)
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } else {
                webClient.put()
                        .uri(url)
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            }
            log.info("Webhook notification sent successfully to {}", url);
            return true;
        } catch (Exception e) {
            log.error("Failed to send webhook notification", e);
            return false;
        }
    }
}
