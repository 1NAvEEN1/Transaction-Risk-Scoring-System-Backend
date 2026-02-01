package com.app.risk.engine;

import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionInput;
import com.app.risk.entity.*;
import com.app.risk.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FrequencyEvaluator Unit Tests")
class FrequencyEvaluatorTest {

    @Mock
    private TransactionRepository transactionRepository;

    private FrequencyEvaluator evaluator;
    private Customer testCustomer;
    private RiskRule frequencyRule;

    @BeforeEach
    void setUp() {
        evaluator = new FrequencyEvaluator(transactionRepository);

        testCustomer = Customer.builder()
                .id(1L)
                .name("Test Customer")
                .email("test@example.com")
                .riskProfile(RiskProfile.LOW)
                .country("USA")
                .build();

        frequencyRule = RiskRule.builder()
                .id(1L)
                .ruleName("High Frequency")
                .ruleType(RuleType.FREQUENCY)
                .frequencyCount(3)
                .frequencyWindowMinutes(10)
                .riskPoints(30)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should support FREQUENCY rule type")
    void testSupportsFrequencyRuleType() {
        assertTrue(evaluator.supports(RuleType.FREQUENCY));
    }

    @Test
    @DisplayName("Should not support other rule types")
    void testDoesNotSupportOtherRuleTypes() {
        assertFalse(evaluator.supports(RuleType.AMOUNT_THRESHOLD));
        assertFalse(evaluator.supports(RuleType.MERCHANT_CATEGORY));
    }

    @Test
    @DisplayName("Should return matched rule when frequency exceeds threshold")
    void testFrequencyExceedsThreshold() {
        // Arrange
        LocalDateTime transactionTime = LocalDateTime.of(2026, 2, 2, 10, 30, 0);
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(transactionTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        // Mock 4 transactions in the last 10 minutes (more than threshold of 3)
        when(transactionRepository.countByCustomerIdAndTimestampAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(4L);

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, frequencyRule);

        // Assert
        assertTrue(result.isPresent());
        MatchedRule matchedRule = result.get();
        assertEquals(1L, matchedRule.getRuleId());
        assertEquals("High Frequency", matchedRule.getRuleName());
        assertEquals("FREQUENCY", matchedRule.getRuleType());
        assertEquals(30, matchedRule.getPoints());
        assertTrue(matchedRule.getReason().contains("4"));
        assertTrue(matchedRule.getReason().contains("10"));
        assertTrue(matchedRule.getReason().contains("3"));
    }

    @Test
    @DisplayName("Should return empty when frequency equals threshold")
    void testFrequencyEqualsThreshold() {
        // Arrange
        LocalDateTime transactionTime = LocalDateTime.of(2026, 2, 2, 10, 30, 0);
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(transactionTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        // Mock exactly 3 transactions in the last 10 minutes (equals threshold)
        when(transactionRepository.countByCustomerIdAndTimestampAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(3L);

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, frequencyRule);

        // Assert
        assertFalse(result.isPresent()); // "more than X" means > X, not >= X
    }

    @Test
    @DisplayName("Should return empty when frequency is below threshold")
    void testFrequencyBelowThreshold() {
        // Arrange
        LocalDateTime transactionTime = LocalDateTime.of(2026, 2, 2, 10, 30, 0);
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(transactionTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        // Mock 2 transactions in the last 10 minutes (below threshold of 3)
        when(transactionRepository.countByCustomerIdAndTimestampAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(2L);

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, frequencyRule);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when rule has no frequency count configured")
    void testRuleWithNoFrequencyCount() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        RiskRule ruleWithoutCount = RiskRule.builder()
                .id(2L)
                .ruleName("Invalid Rule")
                .ruleType(RuleType.FREQUENCY)
                .frequencyCount(null)
                .frequencyWindowMinutes(10)
                .riskPoints(30)
                .active(true)
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, ruleWithoutCount);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when rule has no frequency window configured")
    void testRuleWithNoFrequencyWindow() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        RiskRule ruleWithoutWindow = RiskRule.builder()
                .id(3L)
                .ruleName("Invalid Rule")
                .ruleType(RuleType.FREQUENCY)
                .frequencyCount(3)
                .frequencyWindowMinutes(null)
                .riskPoints(30)
                .active(true)
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, ruleWithoutWindow);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when timestamp format is invalid")
    void testInvalidTimestampFormat() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp("invalid-timestamp")
                .merchantCategory("RETAIL")
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, frequencyRule);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should handle zero transactions in window")
    void testZeroTransactionsInWindow() {
        // Arrange
        LocalDateTime transactionTime = LocalDateTime.of(2026, 2, 2, 10, 30, 0);
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(transactionTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        // Mock 0 transactions in the last 10 minutes
        when(transactionRepository.countByCustomerIdAndTimestampAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0L);

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, frequencyRule);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should handle very short time window")
    void testVeryShortTimeWindow() {
        // Arrange
        LocalDateTime transactionTime = LocalDateTime.of(2026, 2, 2, 10, 30, 0);
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(transactionTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        RiskRule shortWindowRule = RiskRule.builder()
                .id(4L)
                .ruleName("Very High Frequency")
                .ruleType(RuleType.FREQUENCY)
                .frequencyCount(1)
                .frequencyWindowMinutes(1)
                .riskPoints(50)
                .active(true)
                .build();

        when(transactionRepository.countByCustomerIdAndTimestampAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(2L);

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, shortWindowRule);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(50, result.get().getPoints());
        assertTrue(result.get().getReason().contains("1 minutes"));
    }

    @Test
    @DisplayName("Should handle very long time window")
    void testVeryLongTimeWindow() {
        // Arrange
        LocalDateTime transactionTime = LocalDateTime.of(2026, 2, 2, 10, 30, 0);
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(transactionTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        RiskRule longWindowRule = RiskRule.builder()
                .id(5L)
                .ruleName("Daily Frequency Limit")
                .ruleType(RuleType.FREQUENCY)
                .frequencyCount(50)
                .frequencyWindowMinutes(1440) // 24 hours
                .riskPoints(60)
                .active(true)
                .build();

        when(transactionRepository.countByCustomerIdAndTimestampAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(51L);

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, longWindowRule);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(60, result.get().getPoints());
        assertTrue(result.get().getReason().contains("1440"));
    }

    @Test
    @DisplayName("Should handle high frequency count correctly")
    void testHighFrequencyCount() {
        // Arrange
        LocalDateTime transactionTime = LocalDateTime.of(2026, 2, 2, 10, 30, 0);
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(transactionTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        RiskRule highFrequencyRule = RiskRule.builder()
                .id(6L)
                .ruleName("Very High Frequency Threshold")
                .ruleType(RuleType.FREQUENCY)
                .frequencyCount(100)
                .frequencyWindowMinutes(60)
                .riskPoints(100)
                .active(true)
                .build();

        when(transactionRepository.countByCustomerIdAndTimestampAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(101L);

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, highFrequencyRule);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(100, result.get().getPoints());
    }
}

