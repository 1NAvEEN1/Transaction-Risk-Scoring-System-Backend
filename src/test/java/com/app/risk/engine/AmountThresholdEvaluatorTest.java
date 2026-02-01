package com.app.risk.engine;

import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionInput;
import com.app.risk.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AmountThresholdEvaluator Unit Tests")
class AmountThresholdEvaluatorTest {

    private AmountThresholdEvaluator evaluator;
    private Customer testCustomer;
    private RiskRule testRule;

    @BeforeEach
    void setUp() {
        evaluator = new AmountThresholdEvaluator();

        testCustomer = Customer.builder()
                .id(1L)
                .name("Test Customer")
                .email("test@example.com")
                .riskProfile(RiskProfile.LOW)
                .country("USA")
                .build();

        testRule = RiskRule.builder()
                .id(1L)
                .ruleName("High Amount Transaction")
                .ruleType(RuleType.AMOUNT_THRESHOLD)
                .amountThreshold(new BigDecimal("10000.00"))
                .riskPoints(50)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should support AMOUNT_THRESHOLD rule type")
    void testSupportsAmountThresholdRuleType() {
        assertTrue(evaluator.supports(RuleType.AMOUNT_THRESHOLD));
    }

    @Test
    @DisplayName("Should not support other rule types")
    void testDoesNotSupportOtherRuleTypes() {
        assertFalse(evaluator.supports(RuleType.MERCHANT_CATEGORY));
        assertFalse(evaluator.supports(RuleType.FREQUENCY));
    }

    @Test
    @DisplayName("Should return matched rule when amount exceeds threshold")
    void testAmountExceedsThreshold() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("15000.00"))
                .currency("USD")

                .merchantCategory("RETAIL")
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, testRule, LocalDateTime.now());

        // Assert
        assertTrue(result.isPresent());
        MatchedRule matchedRule = result.get();
        assertEquals(1L, matchedRule.getRuleId());
        assertEquals("High Amount Transaction", matchedRule.getRuleName());
        assertEquals("AMOUNT_THRESHOLD", matchedRule.getRuleType());
        assertEquals(50, matchedRule.getPoints());
        assertTrue(matchedRule.getReason().contains("15000.00"));
        assertTrue(matchedRule.getReason().contains("10000.00"));
    }

    @Test
    @DisplayName("Should return empty when amount equals threshold")
    void testAmountEqualsThreshold() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("10000.00"))
                .currency("USD")
                
                .merchantCategory("RETAIL")
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, testRule, LocalDateTime.now());

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when amount is below threshold")
    void testAmountBelowThreshold() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("5000.00"))
                .currency("USD")
                
                .merchantCategory("RETAIL")
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, testRule, LocalDateTime.now());

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when rule has no amount threshold configured")
    void testRuleWithNoAmountThreshold() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("15000.00"))
                .currency("USD")
                
                .merchantCategory("RETAIL")
                .build();

        RiskRule ruleWithoutThreshold = RiskRule.builder()
                .id(2L)
                .ruleName("Invalid Rule")
                .ruleType(RuleType.AMOUNT_THRESHOLD)
                .amountThreshold(null)
                .riskPoints(50)
                .active(true)
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, ruleWithoutThreshold, LocalDateTime.now());

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should handle very small amounts correctly")
    void testVerySmallAmount() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("0.01"))
                .currency("USD")
                
                .merchantCategory("RETAIL")
                .build();

        RiskRule smallThresholdRule = RiskRule.builder()
                .id(3L)
                .ruleName("Micro Transaction")
                .ruleType(RuleType.AMOUNT_THRESHOLD)
                .amountThreshold(new BigDecimal("0.01"))
                .riskPoints(10)
                .active(true)
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, smallThresholdRule, LocalDateTime.now());

        // Assert
        assertFalse(result.isPresent()); // 0.01 is not greater than 0.01
    }

    @Test
    @DisplayName("Should handle very large amounts correctly")
    void testVeryLargeAmount() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("999999999.99"))
                .currency("USD")
                
                .merchantCategory("RETAIL")
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, testRule, LocalDateTime.now());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(50, result.get().getPoints());
    }

    @Test
    @DisplayName("Should handle decimal precision correctly")
    void testDecimalPrecision() {
        // Arrange - Amount just slightly above threshold
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("10000.01"))
                .currency("USD")

                .merchantCategory("RETAIL")
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, testRule, LocalDateTime.now());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(50, result.get().getPoints());
    }
}

