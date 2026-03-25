package com.jvmd.transationapp.service.notification.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.config.TelegramConfig;
import com.jvmd.transationapp.model.NotificationConfig;
import com.jvmd.transationapp.model.Transactions;
import com.jvmd.transationapp.service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramNotificationSender implements NotificationSender {

    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final TelegramConfig tgConfig;


    public boolean send(NotificationConfig config, String message, Transactions transaction) {
        if (!tgConfig.isEnabled() || tgConfig.getBotToken() == null || tgConfig.getBotToken().isEmpty()) {
            log.warn("Telegram notifications are disabled or bot token is not configured");
            return false;
        }

        try {
            Map<String, Object> telegramConfig = objectMapper.readValue(
                    config.getConfiguration(),
                    new TypeReference<>() {
                    }
            );

            String chatId = (String) telegramConfig.get("chatId");

            Map<String, Object> request = new HashMap<>();
            request.put("chat_id", chatId);
            request.put("text", message);
            request.put("parse_mode", "HTML");

            WebClient webClient = webClientBuilder.build();
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", tgConfig.getBotToken());

            webClient.post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Telegram message sent successfully to chat {}", chatId);
            return true;

        } catch (Exception e) {
            log.error("Failed to send Telegram notification", e);
            return false;
        }
    }
}
