package com.app.risk.service;

import com.app.risk.audit.AuditLogService;
import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionDTO;
import com.app.risk.dto.TransactionInput;
import com.app.risk.dto.TransactionPage;
import com.app.risk.engine.RiskRuleEvaluator;
import com.app.risk.exception.BadRequestException;
import com.app.risk.exception.NotFoundException;
import com.app.risk.entity.*;
import com.app.risk.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerService customerService;
    private final RiskRuleService riskRuleService;
    private final List<RiskRuleEvaluator> riskRuleEvaluators;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    private static final int FLAGGED_THRESHOLD = 70;

    public TransactionPage getTransactions(Integer page, Integer size, String status, String searchQuery) {
        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 10,
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Page<Transaction> transactionPage;
        TransactionStatus transactionStatus = null;

        if (status != null && !status.isEmpty()) {
            try {
                transactionStatus = TransactionStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + status);
            }
        }

        // Use the new search method that handles both status and searchQuery
        transactionPage = transactionRepository.findByStatusAndCustomerSearch(
            transactionStatus,
            searchQuery,
            pageable
        );

        List<TransactionDTO> content = transactionPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return TransactionPage.builder()
                .content(content)
                .page(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .build();
    }

    public TransactionDTO getTransaction(Long id) {
        log.debug("Retrieving transaction with id: {}", id);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Transaction not found with id: {}", id);
                    return new NotFoundException("Transaction not found with id: " + id);
                });

        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("transactionId", id);
        auditDetails.put("customerId", transaction.getCustomer().getId());
        auditDetails.put("riskScore", transaction.getRiskScore());
        auditDetails.put("status", transaction.getStatus().name());

        auditLogService.logCustomEvent(
                "TRANSACTION_RETRIEVED",
                "GET_TRANSACTION",
                "Transaction",
                id,
                "SUCCESS",
                auditDetails
        );

        return toDTO(transaction);
    }

    @Transactional
    public TransactionDTO submitTransaction(TransactionInput input) {
        long startTime = System.currentTimeMillis();

        log.info("Processing transaction submission for customer: {}", input.getCustomerId());

        try {
            // Validate customer exists
            Customer customer = customerService.findById(input.getCustomerId());

            // Validate merchant category
            MerchantCategory merchantCategory;
            try {
                merchantCategory = MerchantCategory.valueOf(input.getMerchantCategory());
            } catch (IllegalArgumentException e) {
                log.error("Invalid merchant category: {}", input.getMerchantCategory());
                auditLogService.logError("SUBMIT_TRANSACTION", "Transaction", null,
                    "Invalid merchant category: " + input.getMerchantCategory(), e);
                throw new BadRequestException("Invalid merchant category: " + input.getMerchantCategory());
            }

            // Get current timestamp in Sri Lanka timezone (UTC+5:30)
            ZonedDateTime sriLankaTime = ZonedDateTime.now(ZoneId.of("Asia/Colombo"));
            LocalDateTime timestamp = sriLankaTime.toLocalDateTime();

            log.debug("Transaction timestamp set to Sri Lanka time: {}", timestamp);

            // Evaluate risk rules
            List<RiskRule> activeRules = riskRuleService.getActiveRules();
            List<MatchedRule> matchedRules = new ArrayList<>();

            log.debug("Evaluating {} active risk rules for transaction", activeRules.size());

            for (RiskRule rule : activeRules) {
                for (RiskRuleEvaluator evaluator : riskRuleEvaluators) {
                    if (evaluator.supports(rule.getRuleType())) {
                        evaluator.evaluate(input, customer, rule, timestamp)
                                .ifPresent(matchedRule -> {
                                    matchedRules.add(matchedRule);
                                    log.debug("Rule matched: {} - {} points", matchedRule.getRuleName(),
                                            matchedRule.getPoints());
                                });
                        break;
                    }
                }
            }

            // Calculate total risk score
            int totalRiskScore = matchedRules.stream()
                    .mapToInt(MatchedRule::getPoints)
                    .sum();

            // Determine status
            TransactionStatus status = totalRiskScore >= FLAGGED_THRESHOLD ?
                    TransactionStatus.FLAGGED : TransactionStatus.APPROVED;

            log.info("Transaction risk evaluation complete. Score: {}, Status: {}, Matched rules: {}",
                    totalRiskScore, status, matchedRules.size());

            // Convert matched rules to JSON
            String matchedRulesJson;
            try {
                matchedRulesJson = objectMapper.writeValueAsString(matchedRules);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize matched rules", e);
                throw new RuntimeException("Failed to serialize matched rules", e);
            }

            // Create and save transaction
            Transaction transaction = Transaction.builder()
                    .customer(customer)
                    .amount(input.getAmount())
                    .currency(input.getCurrency())
                    .timestamp(timestamp)
                    .merchantCategory(merchantCategory)
                    .riskScore(totalRiskScore)
                    .matchedRulesJson(matchedRulesJson)
                    .status(status)
                    .build();

            Transaction savedTransaction = transactionRepository.save(transaction);

            long executionTime = System.currentTimeMillis() - startTime;

            // Audit logging
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("amount", input.getAmount());
            auditDetails.put("currency", input.getCurrency());
            auditDetails.put("merchantCategory", merchantCategory.name());
            auditDetails.put("riskScore", totalRiskScore);
            auditDetails.put("status", status.name());
            auditDetails.put("matchedRulesCount", matchedRules.size());
            auditDetails.put("executionTimeMs", executionTime);

            auditLogService.logTransactionSubmitted(savedTransaction.getId(), customer.getId(),
                    customer.getEmail(), auditDetails);

            // Log risk evaluation details
            auditLogService.logRiskEvaluation(savedTransaction.getId(), customer.getId(),
                    totalRiskScore, activeRules.size(), matchedRules.size(), executionTime);

            // Log if transaction was flagged
            if (status == TransactionStatus.FLAGGED) {
                Map<String, Object> flagDetails = new HashMap<>();
                flagDetails.put("amount", input.getAmount());
                flagDetails.put("merchantCategory", merchantCategory.name());
                flagDetails.put("matchedRules", matchedRules.stream()
                        .map(MatchedRule::getRuleName)
                        .collect(Collectors.toList()));

                auditLogService.logTransactionFlagged(savedTransaction.getId(), customer.getId(),
                        totalRiskScore, matchedRules.size(), flagDetails);

                log.warn("Transaction {} flagged for review. Customer: {}, Score: {}",
                        savedTransaction.getId(), customer.getEmail(), totalRiskScore);
            } else {
                auditLogService.logTransactionApproved(savedTransaction.getId(), customer.getId(),
                        totalRiskScore);
            }

            log.info("Transaction {} processed successfully in {}ms", savedTransaction.getId(), executionTime);

            return toDTO(savedTransaction);

        } catch (BadRequestException | NotFoundException e) {
            log.error("Transaction submission failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during transaction submission", e);
            auditLogService.logError("SUBMIT_TRANSACTION", "Transaction", null,
                    "Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    private TransactionDTO toDTO(Transaction transaction) {
        List<MatchedRule> matchedRules = new ArrayList<>();
        if (transaction.getMatchedRulesJson() != null && !transaction.getMatchedRulesJson().isEmpty()) {
            try {
                matchedRules = objectMapper.readValue(
                        transaction.getMatchedRulesJson(),
                        new TypeReference<List<MatchedRule>>() {}
                );
            } catch (JsonProcessingException e) {
                // Log error and continue with empty list
            }
        }

        return TransactionDTO.builder()
                .id(transaction.getId())
                .customerId(transaction.getCustomer().getId())
                .customerName(transaction.getCustomer().getName())
                .customerEmail(transaction.getCustomer().getEmail())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .timestamp(transaction.getTimestamp())
                .merchantCategory(transaction.getMerchantCategory().name())
                .riskScore(transaction.getRiskScore())
                .status(transaction.getStatus().name())
                .matchedRules(matchedRules)
                .build();
    }
}

