package com.jvmd.transationapp.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
@Entity
@Table(name = "rules")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Rule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType type;  
    @Column(nullable = false, columnDefinition = "TEXT")
    private String configuration;  
    @Column(nullable = false)
    private Boolean enabled = true;
    @Column(nullable = false)
    private Integer priority = 0;  
    @Column(nullable = false)
    private Integer severity = 1;  
    @Column
    private String createdBy;
    @Column
    private String modifiedBy;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Column
    private Integer executionCount = 0;
    @Column
    private Integer alertCount = 0;
}
