package com.jvmd.transationapp.service.notification;

import com.jvmd.transationapp.model.NotificationConfig;
import com.jvmd.transationapp.model.Transactions;

public interface NotificationSender {
    boolean send(NotificationConfig config, String message, Transactions transaction);
}
