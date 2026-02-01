package com.app.risk.seed;

import com.app.risk.model.*;
import com.app.risk.repository.CustomerRepository;
import com.app.risk.repository.RiskRuleRepository;
import com.app.risk.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final RiskRuleRepository riskRuleRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (customerRepository.count() > 0) {
            log.info("Data already initialized. Skipping seed data...");
            return;
        }

        log.info("Initializing seed data...");

        // Create customers
        Customer customer1 = customerRepository.save(Customer.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .riskProfile(RiskProfile.LOW)
                .country("USA")
                .build());

        Customer customer2 = customerRepository.save(Customer.builder()
                .name("Jane Smith")
                .email("jane.smith@example.com")
                .riskProfile(RiskProfile.MEDIUM)
                .country("UK")
                .build());

        Customer customer3 = customerRepository.save(Customer.builder()
                .name("Bob Johnson")
                .email("bob.johnson@example.com")
                .riskProfile(RiskProfile.HIGH)
                .country("Canada")
                .build());

        log.info("Created {} customers", customerRepository.count());

        // Create risk rules
        RiskRule highAmountRule = riskRuleRepository.save(RiskRule.builder()
                .ruleName("High Amount")
                .ruleType(RuleType.AMOUNT_THRESHOLD)
                .amountThreshold(new BigDecimal("10000"))
                .riskPoints(50)
                .active(true)
                .build());

        RiskRule gamblingRule = riskRuleRepository.save(RiskRule.builder()
                .ruleName("Gambling")
                .ruleType(RuleType.MERCHANT_CATEGORY)
                .merchantCategory(MerchantCategory.GAMBLING)
                .riskPoints(40)
                .active(true)
                .build());

        RiskRule frequencyRule = riskRuleRepository.save(RiskRule.builder()
                .ruleName("High Frequency")
                .ruleType(RuleType.FREQUENCY)
                .frequencyCount(3)
                .frequencyWindowMinutes(10)
                .riskPoints(30)
                .active(true)
                .build());

        log.info("Created {} risk rules", riskRuleRepository.count());

        // Create transactions
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> transactions = new ArrayList<>();

        // Normal transactions - APPROVED
        for (int i = 0; i < 10; i++) {
            transactions.add(Transaction.builder()
                    .customer(customer1)
                    .amount(new BigDecimal("50.00"))
                    .currency("USD")
                    .timestamp(now.minusHours(i))
                    .merchantCategory(MerchantCategory.RETAIL)
                    .riskScore(0)
                    .matchedRulesJson("[]")
                    .status(TransactionStatus.APPROVED)
                    .build());
        }

        // High amount transaction - APPROVED (score = 50)
        transactions.add(Transaction.builder()
                .customer(customer2)
                .amount(new BigDecimal("12000.00"))
                .currency("USD")
                .timestamp(now.minusHours(5))
                .merchantCategory(MerchantCategory.RETAIL)
                .riskScore(50)
                .matchedRulesJson("[{\"ruleId\":" + highAmountRule.getId() +
                        ",\"ruleName\":\"High Amount\",\"ruleType\":\"AMOUNT_THRESHOLD\"," +
                        "\"points\":50,\"reason\":\"Transaction amount 12000.00 exceeds threshold 10000\"}]")
                .status(TransactionStatus.APPROVED)
                .build());

        // Gambling transaction - APPROVED (score = 40)
        transactions.add(Transaction.builder()
                .customer(customer2)
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .timestamp(now.minusHours(3))
                .merchantCategory(MerchantCategory.GAMBLING)
                .riskScore(40)
                .matchedRulesJson("[{\"ruleId\":" + gamblingRule.getId() +
                        ",\"ruleName\":\"Gambling\",\"ruleType\":\"MERCHANT_CATEGORY\"," +
                        "\"points\":40,\"reason\":\"High-risk merchant category: GAMBLING\"}]")
                .status(TransactionStatus.APPROVED)
                .build());

        // High amount + Gambling - FLAGGED (score = 90)
        transactions.add(Transaction.builder()
                .customer(customer3)
                .amount(new BigDecimal("15000.00"))
                .currency("USD")
                .timestamp(now.minusHours(2))
                .merchantCategory(MerchantCategory.GAMBLING)
                .riskScore(90)
                .matchedRulesJson("[{\"ruleId\":" + highAmountRule.getId() +
                        ",\"ruleName\":\"High Amount\",\"ruleType\":\"AMOUNT_THRESHOLD\"," +
                        "\"points\":50,\"reason\":\"Transaction amount 15000.00 exceeds threshold 10000\"}," +
                        "{\"ruleId\":" + gamblingRule.getId() +
                        ",\"ruleName\":\"Gambling\",\"ruleType\":\"MERCHANT_CATEGORY\"," +
                        "\"points\":40,\"reason\":\"High-risk merchant category: GAMBLING\"}]")
                .status(TransactionStatus.FLAGGED)
                .build());

        // Crypto transactions - APPROVED
        for (int i = 0; i < 5; i++) {
            transactions.add(Transaction.builder()
                    .customer(customer1)
                    .amount(new BigDecimal("200.00"))
                    .currency("USD")
                    .timestamp(now.minusMinutes(30 + i * 5))
                    .merchantCategory(MerchantCategory.CRYPTO)
                    .riskScore(0)
                    .matchedRulesJson("[]")
                    .status(TransactionStatus.APPROVED)
                    .build());
        }

        // Frequency scenario - setup for testing
        // Create 4 transactions within 10 minutes (will trigger frequency rule on 5th)
        LocalDateTime baseTime = now.minusMinutes(15);
        for (int i = 0; i < 4; i++) {
            transactions.add(Transaction.builder()
                    .customer(customer3)
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .timestamp(baseTime.plusMinutes(i * 2))
                    .merchantCategory(MerchantCategory.RETAIL)
                    .riskScore(0)
                    .matchedRulesJson("[]")
                    .status(TransactionStatus.APPROVED)
                    .build());
        }

        // Misc transactions
        transactions.add(Transaction.builder()
                .customer(customer2)
                .amount(new BigDecimal("5000.00"))
                .currency("EUR")
                .timestamp(now.minusDays(1))
                .merchantCategory(MerchantCategory.OTHER)
                .riskScore(0)
                .matchedRulesJson("[]")
                .status(TransactionStatus.APPROVED)
                .build());

        transactions.add(Transaction.builder()
                .customer(customer1)
                .amount(new BigDecimal("9999.99"))
                .currency("USD")
                .timestamp(now.minusDays(2))
                .merchantCategory(MerchantCategory.RETAIL)
                .riskScore(0)
                .matchedRulesJson("[]")
                .status(TransactionStatus.APPROVED)
                .build());

        transactionRepository.saveAll(transactions);
        log.info("Created {} transactions", transactionRepository.count());
        log.info("Seed data initialization completed!");
    }
}

