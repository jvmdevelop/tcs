package com.jvmd.transationapp.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "notification_config")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class NotificationConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;  
    @Column(nullable = false)
    private Boolean enabled = true;
    @Column(nullable = false)
    private Integer minSeverity = 1;  
    @Column(nullable = false, columnDefinition = "TEXT")
    private String configuration;  
    @Column(columnDefinition = "TEXT")
    private String messageTemplate;
}
