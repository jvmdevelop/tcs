package com.jvmd.transationapp.dto;

import com.jvmd.transationapp.model.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResponse {
    private Long id;
    private String name;
    private String description;
    private RuleType type;
    private String configuration;
    private Boolean enabled;
    private Integer priority;
    private Integer severity;
    private String createdBy;
    private String modifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer executionCount;
    private Integer alertCount;
}
