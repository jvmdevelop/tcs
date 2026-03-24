package com.jvmd.transationapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalProcessed;
    private long totalAlerted;
    private long totalReviewed;
    private long totalProcessing;
    private int activeRulesCount;
}
