package com.app.risk.engine;

import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionInput;
import com.app.risk.entity.Customer;
import com.app.risk.entity.RiskRule;
import com.app.risk.entity.RuleType;
import com.app.risk.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FrequencyEvaluator implements RiskRuleEvaluator {

    private final TransactionRepository transactionRepository;

    @Override
    public boolean supports(RuleType ruleType) {
        return ruleType == RuleType.FREQUENCY;
    }

    @Override
    public Optional<MatchedRule> evaluate(TransactionInput input, Customer customer, RiskRule rule, LocalDateTime timestamp) {
        if (rule.getFrequencyCount() == null || rule.getFrequencyWindowMinutes() == null) {
            return Optional.empty();
        }

        LocalDateTime cutoffTime = timestamp.minusMinutes(rule.getFrequencyWindowMinutes());

        long transactionCount = transactionRepository.countByCustomerIdAndTimestampAfter(
                customer.getId(), cutoffTime);

        // "more than X" means strictly greater than X
        if (transactionCount > rule.getFrequencyCount()) {
            MatchedRule matchedRule = MatchedRule.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getRuleName())
                    .ruleType(rule.getRuleType().name())
                    .points(rule.getRiskPoints())
                    .reason(String.format("Frequency threshold exceeded: %d transactions in %d minutes (threshold: %d)",
                            transactionCount, rule.getFrequencyWindowMinutes(), rule.getFrequencyCount()))
                    .build();
            return Optional.of(matchedRule);
        }

        return Optional.empty();
    }
}

