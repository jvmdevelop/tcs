package com.jvmd.transationapp.dto;
import com.jvmd.transationapp.model.EStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private UUID id;
    private String correlationId;
    private BigDecimal amount;
    private String from;
    private String to;
    private String type;
    private LocalDateTime timestamp;
    private EStatus status;
    private String message;
}
