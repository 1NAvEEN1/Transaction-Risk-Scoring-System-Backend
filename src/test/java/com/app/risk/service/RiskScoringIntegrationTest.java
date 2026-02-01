package com.app.risk.service;

import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionDTO;
import com.app.risk.dto.TransactionInput;
import com.app.risk.engine.AmountThresholdEvaluator;
import com.app.risk.engine.FrequencyEvaluator;
import com.app.risk.engine.MerchantCategoryEvaluator;
import com.app.risk.engine.RiskRuleEvaluator;
import com.app.risk.entity.*;
import com.app.risk.exception.BadRequestException;
import com.app.risk.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Risk Scoring Logic Integration Tests")
class RiskScoringIntegrationTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private RiskRuleService riskRuleService;

    @Mock
    private FrequencyEvaluator frequencyEvaluator;

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
                objectMapper
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
    @DisplayName("Should calculate risk score 0 for normal transaction and mark as APPROVED")
    void testNormalTransaction_ZeroScore_Approved() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getStandardRules());
        when(frequencyEvaluator.supports(RuleType.FREQUENCY)).thenReturn(true);
        when(frequencyEvaluator.evaluate(any(), any(), any())).thenReturn(Optional.empty());
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

        // Verify transaction was saved with correct values
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(0, savedTransaction.getRiskScore());
        assertEquals(TransactionStatus.APPROVED, savedTransaction.getStatus());
    }

    @Test
    @DisplayName("Should calculate risk score 50 for high amount transaction and mark as APPROVED")
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
        when(riskRuleService.getActiveRules()).thenReturn(getStandardRules());
        when(frequencyEvaluator.supports(RuleType.FREQUENCY)).thenReturn(true);
        when(frequencyEvaluator.evaluate(any(), any(), any())).thenReturn(Optional.empty());
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
        assertEquals(50, result.getMatchedRules().get(0).getPoints());
    }

    @Test
    @DisplayName("Should calculate risk score 40 for gambling transaction and mark as APPROVED")
    void testGamblingTransaction_Score40_Approved() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("GAMBLING")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getStandardRules());
        when(frequencyEvaluator.supports(RuleType.FREQUENCY)).thenReturn(true);
        when(frequencyEvaluator.evaluate(any(), any(), any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(3L);
            return t;
        });

        // Act
        TransactionDTO result = transactionService.submitTransaction(input);

        // Assert
        assertNotNull(result);
        assertEquals(40, result.getRiskScore());
        assertEquals("APPROVED", result.getStatus());
        assertEquals(1, result.getMatchedRules().size());
        assertEquals("Gambling", result.getMatchedRules().get(0).getRuleName());
    }

    @Test
    @DisplayName("Should calculate risk score 70 for threshold transaction and mark as FLAGGED")
    void testThresholdTransaction_Score70_Flagged() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("12000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("CRYPTO")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getStandardRules());
        when(frequencyEvaluator.supports(RuleType.FREQUENCY)).thenReturn(true);
        when(frequencyEvaluator.evaluate(any(), any(), any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(4L);
            return t;
        });

        // Act
        TransactionDTO result = transactionService.submitTransaction(input);

        // Assert
        assertNotNull(result);
        assertEquals(70, result.getRiskScore()); // 50 (amount) + 20 (crypto)
        assertEquals("FLAGGED", result.getStatus());
        assertEquals(2, result.getMatchedRules().size());
    }

    @Test
    @DisplayName("Should calculate risk score 120 for multiple rules and mark as FLAGGED")
    void testMultipleRules_Score120_Flagged() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("11000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("GAMBLING")
                .build();

        MatchedRule frequencyMatch = MatchedRule.builder()
                .ruleId(3L)
                .ruleName("High Frequency")
                .ruleType("FREQUENCY")
                .points(30)
                .reason("Frequency threshold exceeded: 5 transactions in 10 minutes")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getStandardRules());
        when(frequencyEvaluator.supports(RuleType.FREQUENCY)).thenReturn(true);
        when(frequencyEvaluator.evaluate(any(), any(), any())).thenReturn(Optional.of(frequencyMatch));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(5L);
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
    @DisplayName("Should calculate risk score 90 for high-risk combination and mark as FLAGGED")
    void testHighRiskCombination_Score90_Flagged() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("15000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("GAMBLING")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getStandardRules());
        when(frequencyEvaluator.supports(RuleType.FREQUENCY)).thenReturn(true);
        when(frequencyEvaluator.evaluate(any(), any(), any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(6L);
            return t;
        });

        // Act
        TransactionDTO result = transactionService.submitTransaction(input);

        // Assert
        assertNotNull(result);
        assertEquals(90, result.getRiskScore()); // 50 (amount) + 40 (gambling)
        assertEquals("FLAGGED", result.getStatus());
        assertEquals(2, result.getMatchedRules().size());
    }

    @Test
    @DisplayName("Should handle invalid merchant category gracefully")
    void testInvalidMerchantCategory() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("INVALID")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            transactionService.submitTransaction(input);
        });
    }

    @Test
    @DisplayName("Should handle invalid timestamp format gracefully")
    void testInvalidTimestampFormat() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp("invalid-timestamp")
                .merchantCategory("RETAIL")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            transactionService.submitTransaction(input);
        });
    }

    @Test
    @DisplayName("Should correctly identify boundary at 69 points as APPROVED")
    void testBoundary_69Points_Approved() {
        // Arrange - Create rules that total exactly 69 points
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("GAMBLING")
                .build();

        MatchedRule frequencyMatch = MatchedRule.builder()
                .ruleId(3L)
                .ruleName("High Frequency")
                .ruleType("FREQUENCY")
                .points(29)
                .reason("Frequency threshold exceeded")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getStandardRules());
        when(frequencyEvaluator.supports(RuleType.FREQUENCY)).thenReturn(true);
        when(frequencyEvaluator.evaluate(any(), any(), any())).thenReturn(Optional.of(frequencyMatch));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(7L);
            return t;
        });

        // Act
        TransactionDTO result = transactionService.submitTransaction(input);

        // Assert
        assertNotNull(result);
        assertEquals(69, result.getRiskScore()); // 40 (gambling) + 29 (frequency)
        assertEquals("APPROVED", result.getStatus());
    }

    @Test
    @DisplayName("Should process transaction with no active rules")
    void testNoActiveRules_ZeroScore() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("99999.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("GAMBLING")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(Arrays.asList());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(8L);
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
    @DisplayName("Should aggregate points correctly from all matched rules")
    void testPointsAggregation() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("20000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("CRYPTO")
                .build();

        MatchedRule frequencyMatch = MatchedRule.builder()
                .ruleId(3L)
                .ruleName("High Frequency")
                .ruleType("FREQUENCY")
                .points(30)
                .reason("Frequency threshold exceeded")
                .build();

        when(customerService.findById(1L)).thenReturn(testCustomer);
        when(riskRuleService.getActiveRules()).thenReturn(getStandardRules());
        when(frequencyEvaluator.supports(RuleType.FREQUENCY)).thenReturn(true);
        when(frequencyEvaluator.evaluate(any(), any(), any())).thenReturn(Optional.of(frequencyMatch));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(9L);
            return t;
        });

        // Act
        TransactionDTO result = transactionService.submitTransaction(input);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getRiskScore()); // 50 (amount) + 20 (crypto) + 30 (frequency)
        assertEquals("FLAGGED", result.getStatus());
        assertEquals(3, result.getMatchedRules().size());

        // Verify each rule contribution
        int totalPoints = result.getMatchedRules().stream()
                .mapToInt(MatchedRule::getPoints)
                .sum();
        assertEquals(100, totalPoints);
    }

    private List<RiskRule> getStandardRules() {
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
                        .build(),
                RiskRule.builder()
                        .id(4L)
                        .ruleName("Crypto")
                        .ruleType(RuleType.MERCHANT_CATEGORY)
                        .merchantCategory(MerchantCategory.CRYPTO)
                        .riskPoints(20)
                        .active(true)
                        .build()
        );
    }
}

