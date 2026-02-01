package com.app.risk.engine;

import com.app.risk.dto.MatchedRule;
import com.app.risk.dto.TransactionInput;
import com.app.risk.model.Customer;
import com.app.risk.model.RiskRule;
import com.app.risk.model.RuleType;

import java.util.Optional;

public interface RiskRuleEvaluator {
    boolean supports(RuleType ruleType);
    Optional<MatchedRule> evaluate(TransactionInput input, Customer customer, RiskRule rule);
}

