package com.jvmd.transationapp.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Table(name = "transactions")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Transactions {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String correlationId;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false, name = "account_from")
    private String from;
    @Column(nullable = false, name = "account_to")
    private String to;
    @Column(nullable = false)
    private String type;  
    @Column(nullable = false)
    private LocalDateTime timestamp;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EStatus status = EStatus.PROCESSING;
    @Column
    private Double mlScore;
    @Column(columnDefinition = "TEXT")
    private String alertReasons;  
    @Column(columnDefinition = "TEXT")
    private String processingHistory;  
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Column
    private String ipAddress;
    @Column
    private String deviceId;
    @Column
    private String location;
    @Column
    private String merchantCategory;
    @Column
    private String deviceUsed;
    @Column
    private String fraudType;
    @Column
    private Double timeSinceLastTransaction;
    @Column
    private Double spendingDeviationScore;
    @Column
    private Double velocityScore;
    @Column
    private Double geoAnomalyScore;
    @Column
    private String paymentChannel;
    @Column
    private String deviceHash;
}
