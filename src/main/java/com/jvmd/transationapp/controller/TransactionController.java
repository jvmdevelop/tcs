package com.jvmd.transationapp.controller;

import com.jvmd.transationapp.dto.ErrorResponse;
import com.jvmd.transationapp.dto.TransactionRequest;
import com.jvmd.transationapp.dto.TransactionResponse;
import com.jvmd.transationapp.model.Transactions;
import com.jvmd.transationapp.repository.TransactionRepository;
import com.jvmd.transationapp.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@Slf4j
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionRepository transactionRepository;
    private final QueueService queueService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("component", "api-ingest");
        try {
            log.info("Received transaction: amount={}, from={}, to={}, type={}",
                    request.getAmount(), request.getFrom(), request.getTo(), request.getType());
            Transactions transaction = new Transactions();
            transaction.setCorrelationId(correlationId);
            transaction.setAmount(request.getAmount());
            transaction.setFrom(request.getFrom());
            transaction.setTo(request.getTo());
            transaction.setType(request.getType());
            transaction.setTimestamp(request.getTimestamp());
            transaction.setIpAddress(request.getIpAddress());
            transaction.setDeviceId(request.getDeviceId());
            transaction.setLocation(request.getLocation());
            transaction = transactionRepository.save(transaction);
            log.info("Transaction saved: id={}, correlationId={}", transaction.getId(), correlationId);
            queueService.enqueue(transaction.getId(), correlationId);
            log.info("Transaction enqueued for processing: id={}, correlationId={}",
                    transaction.getId(), correlationId);
            TransactionResponse response = TransactionResponse.builder()
                    .id(transaction.getId())
                    .correlationId(correlationId)
                    .amount(transaction.getAmount())
                    .from(transaction.getFrom())
                    .to(transaction.getTo())
                    .type(transaction.getType())
                    .timestamp(transaction.getTimestamp())
                    .status(transaction.getStatus())
                    .message("Transaction accepted and queued for processing")
                    .build();
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            log.error("Error processing transaction request", e);
            throw new RuntimeException("Failed to process transaction", e);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("component");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID id) {
        return transactionRepository.findById(id)
                .map(transaction -> {
                    TransactionResponse response = TransactionResponse.builder()
                            .id(transaction.getId())
                            .correlationId(transaction.getCorrelationId())
                            .amount(transaction.getAmount())
                            .from(transaction.getFrom())
                            .to(transaction.getTo())
                            .type(transaction.getType())
                            .timestamp(transaction.getTimestamp())
                            .status(transaction.getStatus())
                            .build();
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<TransactionResponse> getTransactionByCorrelationId(@PathVariable String correlationId) {
        return transactionRepository.findByCorrelationId(correlationId)
                .map(transaction -> {
                    TransactionResponse response = TransactionResponse.builder()
                            .id(transaction.getId())
                            .correlationId(transaction.getCorrelationId())
                            .amount(transaction.getAmount())
                            .from(transaction.getFrom())
                            .to(transaction.getTo())
                            .type(transaction.getType())
                            .timestamp(transaction.getTimestamp())
                            .status(transaction.getStatus())
                            .build();
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return fieldName + ": " + errorMessage;
                })
                .collect(Collectors.toList());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid request parameters")
                .details(errors)
                .correlationId(MDC.get("correlationId"))
                .build();
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(ex.getMessage())
                .correlationId(MDC.get("correlationId"))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
