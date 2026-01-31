package com.app.risk.engine;

import com.app.risk.customer.Customer;
import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionInput;
import com.app.risk.rule.RiskRule;
import com.app.risk.rule.RuleType;

import java.util.Optional;

public interface RiskRuleEvaluator {
    boolean supports(RuleType ruleType);
    Optional<MatchedRule> evaluate(TransactionInput input, Customer customer, RiskRule rule);
}

