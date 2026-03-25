package com.jvmd.transationapp.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "notification")
@Validated
@Data
public class NotificationProperties {
    @NotBlank
    private String baseUrl = "http://localhost:8080";
    private int maxRetries = 3;
    private int asyncPoolSize = 4;
}