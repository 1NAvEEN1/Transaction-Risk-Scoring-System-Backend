package com.app.risk.rule;

import com.app.risk.dto.RiskRuleDTO;
import com.app.risk.dto.RiskRuleInput;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RiskRuleResolver {

    private final RiskRuleService riskRuleService;

    @QueryMapping
    public List<RiskRuleDTO> riskRules() {
        return riskRuleService.getAllRules();
    }

    @MutationMapping
    public RiskRuleDTO createRiskRule(@Argument @Valid RiskRuleInput input) {
        return riskRuleService.createRule(input);
    }

    @MutationMapping
    public RiskRuleDTO updateRiskRule(@Argument Long id, @Argument @Valid RiskRuleInput input) {
        return riskRuleService.updateRule(id, input);
    }
}

