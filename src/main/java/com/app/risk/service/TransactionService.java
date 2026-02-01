package com.app.risk.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerService customerService;
    private final RiskRuleService riskRuleService;
    private final List<RiskRuleEvaluator> riskRuleEvaluators;
    private final ObjectMapper objectMapper;

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
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found with id: " + id));
        return toDTO(transaction);
    }

    @Transactional
    public TransactionDTO submitTransaction(TransactionInput input) {
        // Validate customer exists
        Customer customer = customerService.findById(input.getCustomerId());

        // Validate merchant category
        MerchantCategory merchantCategory;
        try {
            merchantCategory = MerchantCategory.valueOf(input.getMerchantCategory());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid merchant category: " + input.getMerchantCategory());
        }

        // Parse timestamp
        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(input.getTimestamp(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            throw new BadRequestException("Invalid timestamp format. Expected ISO_DATE_TIME format.");
        }

        // Evaluate risk rules
        List<RiskRule> activeRules = riskRuleService.getActiveRules();
        List<MatchedRule> matchedRules = new ArrayList<>();

        for (RiskRule rule : activeRules) {
            for (RiskRuleEvaluator evaluator : riskRuleEvaluators) {
                if (evaluator.supports(rule.getRuleType())) {
                    evaluator.evaluate(input, customer, rule)
                            .ifPresent(matchedRules::add);
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

        // Convert matched rules to JSON
        String matchedRulesJson;
        try {
            matchedRulesJson = objectMapper.writeValueAsString(matchedRules);
        } catch (JsonProcessingException e) {
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
        return toDTO(savedTransaction);
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

