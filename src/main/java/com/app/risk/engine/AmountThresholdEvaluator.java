package com.app.risk.engine;

import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionInput;
import com.app.risk.entity.Customer;
import com.app.risk.entity.RiskRule;
import com.app.risk.entity.RuleType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class AmountThresholdEvaluator implements RiskRuleEvaluator {

    @Override
    public boolean supports(RuleType ruleType) {
        return ruleType == RuleType.AMOUNT_THRESHOLD;
    }

    @Override
    public Optional<MatchedRule> evaluate(TransactionInput input, Customer customer, RiskRule rule, LocalDateTime timestamp) {
        if (rule.getAmountThreshold() == null) {
            return Optional.empty();
        }

        if (input.getAmount().compareTo(rule.getAmountThreshold()) > 0) {
            MatchedRule matchedRule = MatchedRule.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getRuleName())
                    .ruleType(rule.getRuleType().name())
                    .points(rule.getRiskPoints())
                    .reason(String.format("Transaction amount %s exceeds threshold %s",
                            input.getAmount(), rule.getAmountThreshold()))
                    .build();
            return Optional.of(matchedRule);
        }

        return Optional.empty();
    }
}

