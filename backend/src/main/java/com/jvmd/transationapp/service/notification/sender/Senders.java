package com.jvmd.transationapp.service.notification.sender;

import com.jvmd.transationapp.model.NotificationChannel;
import com.jvmd.transationapp.service.notification.sender.impl.EmailNotificationSender;
import com.jvmd.transationapp.service.notification.sender.impl.TelegramNotificationSender;
import com.jvmd.transationapp.service.notification.sender.impl.WebhookNotificationSender;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class Senders {
    private final EmailNotificationSender emailNotificationSender;
    private final WebhookNotificationSender webhookNotificationSender;
    private final TelegramNotificationSender telegramNotificationSender;


    public NotificationSender getSender(NotificationChannel notificationChannel) {
        switch (notificationChannel) {
            case EMAIL: return emailNotificationSender;
            case WEBHOOK: return webhookNotificationSender;
            case TELEGRAM: return telegramNotificationSender;
            default: return null;
        }
    }

}
