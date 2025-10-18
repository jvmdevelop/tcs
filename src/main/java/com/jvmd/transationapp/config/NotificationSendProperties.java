package com.jvmd.transationapp.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class NotificationSendProperties {
    @Value("${admin.mail}")
    private String mail;

    @Value("${admin.tg_id}")
    private String tgId;
}
