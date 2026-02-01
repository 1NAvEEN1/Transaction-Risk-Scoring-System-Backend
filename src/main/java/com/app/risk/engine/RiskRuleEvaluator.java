package com.app.risk.engine;

import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionInput;
import com.app.risk.entity.Customer;
import com.app.risk.entity.RiskRule;
import com.app.risk.entity.RuleType;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RiskRuleEvaluator {
    boolean supports(RuleType ruleType);
    Optional<MatchedRule> evaluate(TransactionInput input, Customer customer, RiskRule rule, LocalDateTime timestamp);
}

