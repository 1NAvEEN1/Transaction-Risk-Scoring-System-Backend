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

@DisplayName("MerchantCategoryEvaluator Unit Tests")
class MerchantCategoryEvaluatorTest {

    private MerchantCategoryEvaluator evaluator;
    private Customer testCustomer;
    private RiskRule gamblingRule;
    private RiskRule cryptoRule;

    @BeforeEach
    void setUp() {
        evaluator = new MerchantCategoryEvaluator();

        testCustomer = Customer.builder()
                .id(1L)
                .name("Test Customer")
                .email("test@example.com")
                .riskProfile(RiskProfile.LOW)
                .country("USA")
                .build();

        gamblingRule = RiskRule.builder()
                .id(1L)
                .ruleName("Gambling Merchant")
                .ruleType(RuleType.MERCHANT_CATEGORY)
                .merchantCategory(MerchantCategory.GAMBLING)
                .riskPoints(40)
                .active(true)
                .build();

        cryptoRule = RiskRule.builder()
                .id(2L)
                .ruleName("Crypto Merchant")
                .ruleType(RuleType.MERCHANT_CATEGORY)
                .merchantCategory(MerchantCategory.CRYPTO)
                .riskPoints(35)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should support MERCHANT_CATEGORY rule type")
    void testSupportsMerchantCategoryRuleType() {
        assertTrue(evaluator.supports(RuleType.MERCHANT_CATEGORY));
    }

    @Test
    @DisplayName("Should not support other rule types")
    void testDoesNotSupportOtherRuleTypes() {
        assertFalse(evaluator.supports(RuleType.AMOUNT_THRESHOLD));
        assertFalse(evaluator.supports(RuleType.FREQUENCY));
    }

    @Test
    @DisplayName("Should return matched rule when merchant category matches GAMBLING")
    void testGamblingCategoryMatches() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("GAMBLING")
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, gamblingRule);

        // Assert
        assertTrue(result.isPresent());
        MatchedRule matchedRule = result.get();
        assertEquals(1L, matchedRule.getRuleId());
        assertEquals("Gambling Merchant", matchedRule.getRuleName());
        assertEquals("MERCHANT_CATEGORY", matchedRule.getRuleType());
        assertEquals(40, matchedRule.getPoints());
        assertTrue(matchedRule.getReason().contains("GAMBLING"));
    }

    @Test
    @DisplayName("Should return matched rule when merchant category matches CRYPTO")
    void testCryptoCategoryMatches() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("5000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("CRYPTO")
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, cryptoRule);

        // Assert
        assertTrue(result.isPresent());
        MatchedRule matchedRule = result.get();
        assertEquals(2L, matchedRule.getRuleId());
        assertEquals("Crypto Merchant", matchedRule.getRuleName());
        assertEquals(35, matchedRule.getPoints());
        assertTrue(matchedRule.getReason().contains("CRYPTO"));
    }

    @Test
    @DisplayName("Should return empty when merchant category does not match")
    void testMerchantCategoryDoesNotMatch() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, gamblingRule);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when rule has no merchant category configured")
    void testRuleWithNoMerchantCategory() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("GAMBLING")
                .build();

        RiskRule ruleWithoutCategory = RiskRule.builder()
                .id(3L)
                .ruleName("Invalid Rule")
                .ruleType(RuleType.MERCHANT_CATEGORY)
                .merchantCategory(null)
                .riskPoints(40)
                .active(true)
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, ruleWithoutCategory);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty when merchant category is invalid")
    void testInvalidMerchantCategory() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("INVALID_CATEGORY")
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, gamblingRule);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should handle RETAIL merchant category")
    void testRetailCategoryMatches() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("RETAIL")
                .build();

        RiskRule retailRule = RiskRule.builder()
                .id(4L)
                .ruleName("Retail Merchant")
                .ruleType(RuleType.MERCHANT_CATEGORY)
                .merchantCategory(MerchantCategory.RETAIL)
                .riskPoints(5)
                .active(true)
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, retailRule);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getPoints());
        assertTrue(result.get().getReason().contains("RETAIL"));
    }

    @Test
    @DisplayName("Should handle OTHER merchant category")
    void testOtherCategoryMatches() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("300.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("OTHER")
                .build();

        RiskRule otherRule = RiskRule.builder()
                .id(5L)
                .ruleName("Other Merchant")
                .ruleType(RuleType.MERCHANT_CATEGORY)
                .merchantCategory(MerchantCategory.OTHER)
                .riskPoints(10)
                .active(true)
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, otherRule);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(10, result.get().getPoints());
        assertTrue(result.get().getReason().contains("OTHER"));
    }

    @Test
    @DisplayName("Should be case-sensitive for merchant category")
    void testCaseSensitivity() {
        // Arrange
        TransactionInput input = TransactionInput.builder()
                .customerId(1L)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .merchantCategory("gambling") // lowercase
                .build();

        // Act
        Optional<MatchedRule> result = evaluator.evaluate(input, testCustomer, gamblingRule);

        // Assert
        assertFalse(result.isPresent()); // Should not match due to case sensitivity
    }
}

