package com.app.risk.transaction;

import com.app.risk.audit.AuditLogService;
import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionDTO;
import com.app.risk.dto.TransactionInput;
import com.app.risk.engine.AmountThresholdEvaluator;
import com.app.risk.engine.FrequencyEvaluator;
import com.app.risk.engine.MerchantCategoryEvaluator;
import com.app.risk.engine.RiskRuleEvaluator;
import com.app.risk.entity.*;
import com.app.risk.repository.TransactionRepository;
import com.app.risk.service.CustomerService;
import com.app.risk.service.RiskRuleService;
import com.app.risk.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private RiskRuleService riskRuleService;

    @Mock
    private FrequencyEvaluator frequencyEvaluator;

    @Mock
    private AuditLogService auditLogService;

    private TransactionService transactionService;
    private ObjectMapper objectMapper;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        List<RiskRuleEvaluator> evaluators = Arrays.asList(
                new AmountThresholdEvaluator(),
                new MerchantCategoryEvaluator(),
                frequencyEvaluator
        );

        transactionService = new TransactionService(
                transactionRepository,
                customerService,
                riskRuleService,
                evaluators,
                objectMapper,
                auditLogService
        );

        testCustomer = Customer.builder()
                .id(1L)
                .name("Test Customer")
                .email("test@example.com")
                .riskProfile(RiskProfile.LOW)
                .country("USA")
                .build();
    }

    @Test
    void testNormalTransaction_Score0_Approved() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getActiveRules());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        // Act
        TransactionDTO result = transactionService.submitTransaction(input);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getRiskScore());
        assertEquals("APPROVED", result.getStatus());
        assertTrue(result.getMatchedRules().isEmpty());
    }

    @Test
    void testHighAmountTransaction_Score50_Approved() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("12000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getActiveRules());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(2L);
            return t;
        });

        // Act
        TransactionDTO result = transactionService.submitTransaction(input);

        // Assert
        assertNotNull(result);
        assertEquals(50, result.getRiskScore());
        assertEquals("APPROVED", result.getStatus());
        assertEquals(1, result.getMatchedRules().size());
        assertEquals("High Amount", result.getMatchedRules().get(0).getRuleName());
    }

    @Test
    void testMultipleRules_Score120_Flagged() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("11000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("GAMBLING")
                .build();

        // Mock frequency evaluator to return a match
        MatchedRule frequencyMatch = MatchedRule.builder()
                .ruleId(3L)
                .ruleName("High Frequency")
                .ruleType("FREQUENCY")
                .points(30)
                .reason("Frequency threshold exceeded")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getActiveRules());
        when(frequencyEvaluator.supports(RuleType.FREQUENCY)).thenReturn(true);
        when(frequencyEvaluator.evaluate(any(), any(), any())).thenReturn(java.util.Optional.of(frequencyMatch));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(3L);
            return t;
        });

        // Act
        TransactionDTO result = transactionService.submitTransaction(input);

        // Assert
        assertNotNull(result);
        assertEquals(120, result.getRiskScore()); // 50 (amount) + 40 (gambling) + 30 (frequency)
        assertEquals("FLAGGED", result.getStatus());
        assertEquals(3, result.getMatchedRules().size());
    }

    @Test
    void testFrequencyBoundary_ExactlyAtThreshold_NotTriggered() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        // Mock exactly 3 transactions (threshold is 3, should NOT trigger)
        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getActiveRules());
        when(frequencyEvaluator.supports(RuleType.FREQUENCY)).thenReturn(true);
        when(frequencyEvaluator.evaluate(any(), any(), any())).thenReturn(java.util.Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(4L);
            return t;
        });

        // Act
        TransactionDTO result = transactionService.submitTransaction(input);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getRiskScore());
        assertEquals("APPROVED", result.getStatus());
        // Frequency rule should not be in matched rules since we're exactly at threshold
    }

    private List<RiskRule> getActiveRules() {
        return Arrays.asList(
                RiskRule.builder()
                        .id(1L)
                        .ruleName("High Amount")
                        .ruleType(RuleType.AMOUNT_THRESHOLD)
                        .amountThreshold(new BigDecimal("10000"))
                        .riskPoints(50)
                        .active(true)
                        .build(),
                RiskRule.builder()
                        .id(2L)
                        .ruleName("Gambling")
                        .ruleType(RuleType.MERCHANT_CATEGORY)
                        .merchantCategory(MerchantCategory.GAMBLING)
                        .riskPoints(40)
                        .active(true)
                        .build(),
                RiskRule.builder()
                        .id(3L)
                        .ruleName("High Frequency")
                        .ruleType(RuleType.FREQUENCY)
                        .frequencyCount(3)
                        .frequencyWindowMinutes(10)
                        .riskPoints(30)
                        .active(true)
                        .build()
        );
    }
}

