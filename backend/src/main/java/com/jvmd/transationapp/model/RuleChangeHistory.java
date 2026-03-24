package com.jvmd.transationapp.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity
@Table(name = "rule_change_history")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RuleChangeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long ruleId;
    @Column(nullable = false)
    private String action;  
    @Column(columnDefinition = "TEXT")
    private String oldValue;  
    @Column(columnDefinition = "TEXT")
    private String newValue;  
    @Column
    private String changedBy;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;
    @Column(columnDefinition = "TEXT")
    private String comment;
}
