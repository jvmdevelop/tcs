package com.jvmd.transationapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.model.*;
import com.jvmd.transationapp.repository.*;
import com.jvmd.transationapp.service.MetricsService;
import com.jvmd.transationapp.service.rules.RuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminController {
    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;
    private final RuleChangeHistoryRepository ruleChangeHistoryRepository;
    private final NotificationConfigRepository notificationConfigRepository;
    private final RuleEngine ruleEngine;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String index(Model model) {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalProcessed = transactionRepository.countByStatus(EStatus.PROCESSED);
        long totalAlerted = transactionRepository.countByStatus(EStatus.ALERTED);
        long totalReviewed = transactionRepository.countByStatus(EStatus.REVIEWED);
        long totalProcessing = transactionRepository.countByStatus(EStatus.PROCESSING);
        model.addAttribute("totalProcessed", totalProcessed);
        model.addAttribute("totalAlerted", totalAlerted);
        model.addAttribute("totalReviewed", totalReviewed);
        model.addAttribute("totalProcessing", totalProcessing);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transactions> recentTransactions = transactionRepository.findAll(pageable);
        model.addAttribute("recentTransactions", recentTransactions.getContent());
        List<Rule> activeRules = ruleRepository.findByEnabledTrueOrderByPriorityAsc();
        model.addAttribute("activeRulesCount", activeRules.size());
        return "dashboard";
    }

    @GetMapping("/transactions")
    public String transactions(
            @RequestParam(required = false) EStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transactions> transactions;
        if (status != null) {
            transactions = transactionRepository.findByStatus(status, pageable);
        } else {
            transactions = transactionRepository.findAll(pageable);
        }
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentStatus", status);
        model.addAttribute("statuses", EStatus.values());
        return "transactions";
    }

    @GetMapping("/transactions/{id}")
    public String transactionDetails(@PathVariable UUID id, Model model) {
        Optional<Transactions> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            return "redirect:/admin/transactions";
        }
        Transactions transaction = transactionOpt.get();
        model.addAttribute("transaction", transaction);
        try {
            if (transaction.getAlertReasons() != null && !transaction.getAlertReasons().isEmpty()) {
                List<String> reasons = objectMapper.readValue(
                        transaction.getAlertReasons(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );
                model.addAttribute("alertReasons", reasons);
            }
            if (transaction.getProcessingHistory() != null && !transaction.getProcessingHistory().isEmpty()) {
                List<Map<String, Object>> history = objectMapper.readValue(
                        transaction.getProcessingHistory(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );
                model.addAttribute("processingHistory", history);
            }
        } catch (Exception e) {
            log.error("Error parsing transaction JSON fields", e);
        }
        return "transaction-details";
    }

    @PostMapping("/transactions/{id}/review")
    public String reviewTransaction(@PathVariable UUID id) {
        transactionRepository.findById(id).ifPresent(transaction -> {
            transaction.setStatus(EStatus.REVIEWED);
            transactionRepository.save(transaction);
            metricsService.recordReviewed();
            log.info("Transaction marked as reviewed: id={}", id);
        });
        return "redirect:/admin/transactions/" + id;
    }

    @GetMapping("/rules")
    public String rules(Model model) {
        List<Rule> rules = ruleRepository.findAll(Sort.by(Sort.Direction.ASC, "priority"));
        model.addAttribute("rules", rules);
        model.addAttribute("ruleTypes", RuleType.values());
        return "rules";
    }

    @GetMapping("/rules/new")
    public String newRuleForm(Model model) {
        model.addAttribute("rule", new Rule());
        model.addAttribute("ruleTypes", RuleType.values());
        return "rule-form";
    }

    @GetMapping("/rules/{id}/edit")
    public String editRuleForm(@PathVariable Long id, Model model) {
        Optional<Rule> ruleOpt = ruleRepository.findById(id);
        if (ruleOpt.isEmpty()) {
            return "redirect:/admin/rules";
        }
        model.addAttribute("rule", ruleOpt.get());
        model.addAttribute("ruleTypes", RuleType.values());
        return "rule-form";
    }

    @PostMapping("/rules")
    public String saveRule(@ModelAttribute Rule rule) {
        boolean isNew = rule.getId() == null;
        Rule savedRule = ruleRepository.save(rule);
        RuleChangeHistory history = new RuleChangeHistory();
        history.setRuleId(savedRule.getId());
        history.setAction(isNew ? "CREATE" : "UPDATE");
        history.setChangedBy("admin");
        try {
            history.setNewValue(objectMapper.writeValueAsString(savedRule));
        } catch (Exception e) {
            log.error("Error serializing rule", e);
        }
        ruleChangeHistoryRepository.save(history);
        ruleEngine.reloadRules();
        log.info("Rule saved: id={}, name={}", savedRule.getId(), savedRule.getName());
        return "redirect:/admin/rules";
    }

    @PostMapping("/rules/{id}/toggle")
    public String toggleRule(@PathVariable Long id) {
        ruleRepository.findById(id).ifPresent(rule -> {
            rule.setEnabled(!rule.getEnabled());
            ruleRepository.save(rule);
            RuleChangeHistory history = new RuleChangeHistory();
            history.setRuleId(id);
            history.setAction(rule.getEnabled() ? "ENABLE" : "DISABLE");
            history.setChangedBy("admin");
            ruleChangeHistoryRepository.save(history);
            ruleEngine.reloadRules();
            log.info("Rule toggled: id={}, enabled={}", id, rule.getEnabled());
        });
        return "redirect:/admin/rules";
    }

    @PostMapping("/rules/{id}/delete")
    public String deleteRule(@PathVariable Long id) {
        ruleRepository.findById(id).ifPresent(rule -> {
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
        });
        return "redirect:/admin/rules";
    }

    @GetMapping("/rules/{id}/history")
    public String ruleHistory(@PathVariable Long id, Model model) {
        Optional<Rule> ruleOpt = ruleRepository.findById(id);
        if (ruleOpt.isEmpty()) {
            return "redirect:/admin/rules";
        }
        List<RuleChangeHistory> history = ruleChangeHistoryRepository.findByRuleIdOrderByChangedAtDesc(id);
        model.addAttribute("rule", ruleOpt.get());
        model.addAttribute("history", history);
        return "rule-history";
    }

    @GetMapping("/export/transactions")
    public ResponseEntity<String> exportTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) EStatus status) {
        try {
            List<Transactions> transactions;
            if (startDate != null && endDate != null) {
                transactions = transactionRepository.findTransactionsBetween(startDate, endDate);
            } else {
                transactions = transactionRepository.findAll();
            }
            if (status != null) {
                transactions = transactions.stream()
                        .filter(t -> t.getStatus() == status)
                        .toList();
            }
            StringWriter writer = new StringWriter();
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                    "ID", "Correlation ID", "Amount", "From", "To", "Type", "Timestamp",
                    "Status", "ML Score", "Created At"
            ));
            for (Transactions transaction : transactions) {
                csvPrinter.printRecord(
                        transaction.getId(),
                        transaction.getCorrelationId(),
                        transaction.getAmount(),
                        transaction.getFrom(),
                        transaction.getTo(),
                        transaction.getType(),
                        transaction.getTimestamp(),
                        transaction.getStatus(),
                        transaction.getMlScore(),
                        transaction.getCreatedAt()
                );
            }
            csvPrinter.flush();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "transactions_" + System.currentTimeMillis() + ".csv");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(writer.toString());
        } catch (Exception e) {
            log.error("Error exporting transactions to CSV", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
