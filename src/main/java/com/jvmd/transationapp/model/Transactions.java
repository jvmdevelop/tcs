package com.jvmd.transationapp.model;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Table
public class Transactions {
    @Id
    private UUID id;

    private BigDecimal amount;

    private String from;
    private String to;

    private LocalDateTime dateTime;

    private EStatus status;

    private float mlScore;


}
