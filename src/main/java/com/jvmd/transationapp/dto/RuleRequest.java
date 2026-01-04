package com.jvmd.transationapp.dto;

import com.jvmd.transationapp.model.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleRequest {
    @NotBlank(message = "Rule name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Rule type is required")
    private RuleType type;
    
    @NotBlank(message = "Configuration is required")
    private String configuration;
    
    private Boolean enabled = true;
    private Integer priority = 0;
    private Integer severity = 1;
}
