package com.jvmd.transationapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.dto.*;
import com.jvmd.transationapp.model.*;
import com.jvmd.transationapp.repository.*;
import com.jvmd.transationapp.service.LLMService;
import com.jvmd.transationapp.service.MetricsService;
import com.jvmd.transationapp.service.rules.RuleEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AdminRestController {
    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;
    private final RuleChangeHistoryRepository ruleChangeHistoryRepository;
    private final NotificationConfigRepository notificationConfigRepository;
    private final RuleEngine ruleEngine;
    private final MetricsService metricsService;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        long totalProcessed = transactionRepository.countByStatus(EStatus.PROCESSED);
        long totalAlerted = transactionRepository.countByStatus(EStatus.ALERTED);
        long totalReviewed = transactionRepository.countByStatus(EStatus.REVIEWED);
        long totalProcessing = transactionRepository.countByStatus(EStatus.PROCESSING);
        
        List<Rule> activeRules = ruleRepository.findByEnabledTrueOrderByPriorityAsc();
        
        DashboardStatsResponse response = DashboardStatsResponse.builder()
                .totalProcessed(totalProcessed)
                .totalAlerted(totalAlerted)
                .totalReviewed(totalReviewed)
                .totalProcessing(totalProcessing)
                .activeRulesCount(activeRules.size())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/recent-transactions")
    public ResponseEntity<List<TransactionResponse>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transactions> recentTransactions = transactionRepository.findAll(pageable);
        
        List<TransactionResponse> response = recentTransactions.getContent().stream()
                .map(this::convertToTransactionResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> getTransactions(
            @RequestParam(required = false) EStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Transactions> transactionsPage;
        
        if (status != null) {
            transactionsPage = transactionRepository.findByStatus(status, pageable);
        } else {
            transactionsPage = transactionRepository.findAll(pageable);
        }
        
        List<TransactionResponse> content = transactionsPage.getContent().stream()
                .map(this::convertToTransactionResponse)
                .collect(Collectors.toList());
        
        PageResponse<TransactionResponse> response = PageResponse.<TransactionResponse>builder()
                .content(content)
                .page(transactionsPage.getNumber())
                .size(transactionsPage.getSize())
                .totalElements(transactionsPage.getTotalElements())
                .totalPages(transactionsPage.getTotalPages())
                .first(transactionsPage.isFirst())
                .last(transactionsPage.isLast())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionDetailsResponse> getTransactionDetails(@PathVariable UUID id) {
        Optional<Transactions> transactionOpt = transactionRepository.findById(id);
        
        if (transactionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Transactions transaction = transactionOpt.get();
        TransactionDetailsResponse response = convertToTransactionDetailsResponse(transaction);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/{id}/review")
    public ResponseEntity<TransactionResponse> reviewTransaction(@PathVariable UUID id) {
        Optional<Transactions> transactionOpt = transactionRepository.findById(id);
        
        if (transactionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Transactions transaction = transactionOpt.get();
        transaction.setStatus(EStatus.REVIEWED);
        transaction = transactionRepository.save(transaction);
        metricsService.recordReviewed();
        
        log.info("Transaction marked as reviewed: id={}", id);
        
        return ResponseEntity.ok(convertToTransactionResponse(transaction));
    }

    @GetMapping("/transactions/search")
    public ResponseEntity<PageResponse<TransactionResponse>> searchTransactions(
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transactions> transactionsPage;
        
        if (correlationId != null) {
            transactionsPage = transactionRepository.findAll(pageable);
            transactionsPage = transactionsPage.map(t -> 
                t.getCorrelationId().contains(correlationId) ? t : null
            );
        } else if (startDate != null && endDate != null) {
            List<Transactions> transactions = transactionRepository.findTransactionsBetween(startDate, endDate);
            transactionsPage = transactionRepository.findAll(pageable);
        } else {
            transactionsPage = transactionRepository.findAll(pageable);
        }
        
        List<TransactionResponse> content = transactionsPage.getContent().stream()
                .filter(Objects::nonNull)
                .map(this::convertToTransactionResponse)
                .collect(Collectors.toList());
        
        PageResponse<TransactionResponse> response = PageResponse.<TransactionResponse>builder()
                .content(content)
                .page(transactionsPage.getNumber())
                .size(transactionsPage.getSize())
                .totalElements(transactionsPage.getTotalElements())
                .totalPages(transactionsPage.getTotalPages())
                .first(transactionsPage.isFirst())
                .last(transactionsPage.isLast())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<RuleResponse>> getAllRules(
            @RequestParam(required = false) Boolean enabled) {
        List<Rule> rules;
        
        if (enabled != null && enabled) {
            rules = ruleRepository.findByEnabledTrueOrderByPriorityAsc();
        } else if (enabled != null && !enabled) {
            rules = ruleRepository.findAll(Sort.by(Sort.Direction.ASC, "priority"))
                    .stream()
                    .filter(r -> !r.getEnabled())
                    .collect(Collectors.toList());
        } else {
            rules = ruleRepository.findAll(Sort.by(Sort.Direction.ASC, "priority"));
        }
        
        List<RuleResponse> response = rules.stream()
                .map(this::convertToRuleResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<RuleResponse> getRule(@PathVariable Long id) {
        return ruleRepository.findById(id)
                .map(this::convertToRuleResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rules")
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody RuleRequest request) {
        Rule rule = new Rule();
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setType(request.getType());
        rule.setConfiguration(request.getConfiguration());
        rule.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        rule.setSeverity(request.getSeverity() != null ? request.getSeverity() : 1);
        rule.setCreatedBy("admin");
        
        Rule savedRule = ruleRepository.save(rule);
        
        RuleChangeHistory history = new RuleChangeHistory();
        history.setRuleId(savedRule.getId());
        history.setAction("CREATE");
        history.setChangedBy("admin");
        try {
            history.setNewValue(objectMapper.writeValueAsString(savedRule));
        } catch (Exception e) {
            log.error("Error serializing rule", e);
        }
        ruleChangeHistoryRepository.save(history);
        
        ruleEngine.reloadRules();
        log.info("Rule created: id={}, name={}", savedRule.getId(), savedRule.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(convertToRuleResponse(savedRule));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<RuleResponse> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody RuleRequest request) {
        
        Optional<Rule> ruleOpt = ruleRepository.findById(id);
        if (ruleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Rule rule = ruleOpt.get();
        String oldValue = null;
        try {
            oldValue = objectMapper.writeValueAsString(rule);
        } catch (Exception e) {
            log.error("Error serializing rule", e);
        }
        
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setType(request.getType());
        rule.setConfiguration(request.getConfiguration());
        rule.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        rule.setSeverity(request.getSeverity() != null ? request.getSeverity() : 1);
        rule.setModifiedBy("admin");
        
        Rule savedRule = ruleRepository.save(rule);
        
        RuleChangeHistory history = new RuleChangeHistory();
        history.setRuleId(id);
        history.setAction("UPDATE");
        history.setChangedBy("admin");
        history.setOldValue(oldValue);
        try {
            history.setNewValue(objectMapper.writeValueAsString(savedRule));
        } catch (Exception e) {
            log.error("Error serializing rule", e);
        }
        ruleChangeHistoryRepository.save(history);
        
        ruleEngine.reloadRules();
        log.info("Rule updated: id={}, name={}", savedRule.getId(), savedRule.getName());
        
        return ResponseEntity.ok(convertToRuleResponse(savedRule));
    }

    @PatchMapping("/rules/{id}/toggle")
    public ResponseEntity<RuleResponse> toggleRule(@PathVariable Long id) {
        Optional<Rule> ruleOpt = ruleRepository.findById(id);
        if (ruleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Rule rule = ruleOpt.get();
        rule.setEnabled(!rule.getEnabled());
        rule = ruleRepository.save(rule);
        
        RuleChangeHistory history = new RuleChangeHistory();
        history.setRuleId(id);
        history.setAction(rule.getEnabled() ? "ENABLE" : "DISABLE");
        history.setChangedBy("admin");
        ruleChangeHistoryRepository.save(history);
        
        ruleEngine.reloadRules();
        log.info("Rule toggled: id={}, enabled={}", id, rule.getEnabled());
        
        return ResponseEntity.ok(convertToRuleResponse(rule));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        Optional<Rule> ruleOpt = ruleRepository.findById(id);
        if (ruleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Rule rule = ruleOpt.get();
        
        RuleChangeHistory history = new RuleChangeHistory();
        history.setRuleId(id);
        history.setAction("DELETE");
        history.setChangedBy("admin");
        try {
            history.setOldValue(objectMapper.writeValueAsString(rule));
        } catch (Exception e) {
            log.error("Error serializing rule", e);
        }
        ruleChangeHistoryRepository.save(history);
        
        ruleRepository.deleteById(id);
        ruleEngine.reloadRules();
        log.info("Rule deleted: id={}", id);
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rules/{id}/history")
    public ResponseEntity<List<RuleChangeHistory>> getRuleHistory(@PathVariable Long id) {
        if (!ruleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        List<RuleChangeHistory> history = ruleChangeHistoryRepository.findByRuleIdOrderByChangedAtDesc(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/rules/types")
    public ResponseEntity<List<String>> getRuleTypes() {
        List<String> types = Arrays.stream(RuleType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<String>> getStatuses() {
        List<String> statuses = Arrays.stream(EStatus.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(statuses);
    }

    private TransactionResponse convertToTransactionResponse(Transactions transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .correlationId(transaction.getCorrelationId())
                .amount(transaction.getAmount())
                .from(transaction.getFrom())
                .to(transaction.getTo())
                .type(transaction.getType())
                .timestamp(transaction.getTimestamp())
                .status(transaction.getStatus())
                .build();
    }

    private TransactionDetailsResponse convertToTransactionDetailsResponse(Transactions transaction) {
        List<String> alertReasons = null;
        List<Map<String, Object>> processingHistory = null;
        String aiAnalysis = null;
        
        try {
            if (transaction.getAlertReasons() != null && !transaction.getAlertReasons().isEmpty()) {
                alertReasons = objectMapper.readValue(
                        transaction.getAlertReasons(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );
            }
            if (transaction.getProcessingHistory() != null && !transaction.getProcessingHistory().isEmpty()) {
                processingHistory = objectMapper.readValue(
                        transaction.getProcessingHistory(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );
            }
        } catch (Exception e) {
            log.error("Error parsing transaction JSON fields", e);
        }
        
        try {
            aiAnalysis = llmService.analyzeTransaction(transaction);
        } catch (Exception e) {
            log.error("Error generating AI analysis for transaction: {}", transaction.getId(), e);
            aiAnalysis = "AI analysis unavailable";
        }
        
        return TransactionDetailsResponse.builder()
                .id(transaction.getId())
                .correlationId(transaction.getCorrelationId())
                .amount(transaction.getAmount())
                .from(transaction.getFrom())
                .to(transaction.getTo())
                .type(transaction.getType())
                .timestamp(transaction.getTimestamp())
                .status(transaction.getStatus())
                .mlScore(transaction.getMlScore())
                .alertReasons(alertReasons)
                .processingHistory(processingHistory)
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .ipAddress(transaction.getIpAddress())
                .deviceId(transaction.getDeviceId())
                .location(transaction.getLocation())
                .merchantCategory(transaction.getMerchantCategory())
                .deviceUsed(transaction.getDeviceUsed())
                .fraudType(transaction.getFraudType())
                .timeSinceLastTransaction(transaction.getTimeSinceLastTransaction())
                .spendingDeviationScore(transaction.getSpendingDeviationScore())
                .velocityScore(transaction.getVelocityScore())
                .geoAnomalyScore(transaction.getGeoAnomalyScore())
                .paymentChannel(transaction.getPaymentChannel())
                .deviceHash(transaction.getDeviceHash())
                .aiAnalysis(aiAnalysis)
                .build();
    }

    private RuleResponse convertToRuleResponse(Rule rule) {
        return RuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .type(rule.getType())
                .configuration(rule.getConfiguration())
                .enabled(rule.getEnabled())
                .priority(rule.getPriority())
                .severity(rule.getSeverity())
                .createdBy(rule.getCreatedBy())
                .modifiedBy(rule.getModifiedBy())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .executionCount(rule.getExecutionCount())
                .alertCount(rule.getAlertCount())
                .build();
    }
}
