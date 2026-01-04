package com.jvmd.transationapp.dto;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    @NotBlank(message = "From account is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,50}$", message = "From account must be alphanumeric, 3-50 characters")
    private String from;
    @NotBlank(message = "To account is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,50}$", message = "To account must be alphanumeric, 3-50 characters")
    private String to;
    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(TRANSFER|PAYMENT|WITHDRAWAL|DEPOSIT)$", message = "Type must be one of: TRANSFER, PAYMENT, WITHDRAWAL, DEPOSIT")
    private String type;
    @NotNull(message = "Timestamp is required")
    @PastOrPresent(message = "Timestamp cannot be in the future")
    private LocalDateTime timestamp;
    private String ipAddress;
    private String deviceId;
    private String location;
}
