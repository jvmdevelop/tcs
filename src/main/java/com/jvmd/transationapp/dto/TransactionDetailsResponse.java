package com.jvmd.transationapp.dto;

import com.jvmd.transationapp.model.EStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailsResponse {
    private UUID id;
    private String correlationId;
    private BigDecimal amount;
    private String from;
    private String to;
    private String type;
    private LocalDateTime timestamp;
    private EStatus status;
    private Double mlScore;
    private List<String> alertReasons;
    private List<Map<String, Object>> processingHistory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ipAddress;
    private String deviceId;
    private String location;
    private String merchantCategory;
    private String deviceUsed;
    private String fraudType;
    private Double timeSinceLastTransaction;
    private Double spendingDeviationScore;
    private Double velocityScore;
    private Double geoAnomalyScore;
    private String paymentChannel;
    private String deviceHash;
    private String aiAnalysis;
}
