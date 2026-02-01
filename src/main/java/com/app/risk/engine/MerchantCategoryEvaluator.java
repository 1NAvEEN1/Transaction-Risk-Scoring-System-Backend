package com.app.risk.engine;

import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionInput;
import com.app.risk.entity.Customer;
import com.app.risk.entity.MerchantCategory;
import com.app.risk.entity.RiskRule;
import com.app.risk.entity.RuleType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class MerchantCategoryEvaluator implements RiskRuleEvaluator {

    @Override
    public boolean supports(RuleType ruleType) {
        return ruleType == RuleType.MERCHANT_CATEGORY;
    }

    @Override
    public Optional<MatchedRule> evaluate(TransactionInput input, Customer customer, RiskRule rule, LocalDateTime timestamp) {
        if (rule.getMerchantCategory() == null) {
            return Optional.empty();
        }

        try {
            MerchantCategory inputCategory = MerchantCategory.valueOf(input.getMerchantCategory());

            if (inputCategory == rule.getMerchantCategory()) {
                MatchedRule matchedRule = MatchedRule.builder()
                        .ruleId(rule.getId())
                        .ruleName(rule.getRuleName())
                        .ruleType(rule.getRuleType().name())
                        .points(rule.getRiskPoints())
                        .reason(String.format("High-risk merchant category: %s", inputCategory))
                        .build();
                return Optional.of(matchedRule);
            }
        } catch (IllegalArgumentException e) {
            // Invalid merchant category
            return Optional.empty();
        }

        return Optional.empty();
    }
}

