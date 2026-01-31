package com.app.risk.rule;

import com.app.risk.dto.RiskRuleDTO;
import com.app.risk.dto.RiskRuleInput;
import com.app.risk.exception.BadRequestException;
import com.app.risk.exception.NotFoundException;
import com.app.risk.transaction.MerchantCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskRuleService {

    private final RiskRuleRepository riskRuleRepository;

    public List<RiskRuleDTO> getAllRules() {
        return riskRuleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<RiskRule> getActiveRules() {
        return riskRuleRepository.findByActiveTrue();
    }

    @Transactional
    public RiskRuleDTO createRule(RiskRuleInput input) {
        validateRuleInput(input);

        RiskRule rule = RiskRule.builder()
                .ruleName(input.getRuleName())
                .ruleType(RuleType.valueOf(input.getRuleType()))
                .amountThreshold(input.getAmountThreshold())
                .merchantCategory(input.getMerchantCategory() != null ?
                        MerchantCategory.valueOf(input.getMerchantCategory()) : null)
                .frequencyCount(input.getFrequencyCount())
                .frequencyWindowMinutes(input.getFrequencyWindowMinutes())
                .riskPoints(input.getRiskPoints())
                .active(input.getActive())
                .build();

        RiskRule savedRule = riskRuleRepository.save(rule);
        return toDTO(savedRule);
    }

    @Transactional
    public RiskRuleDTO updateRule(Long id, RiskRuleInput input) {
        RiskRule rule = riskRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Risk rule not found with id: " + id));

        validateRuleInput(input);

        rule.setRuleName(input.getRuleName());
        rule.setRuleType(RuleType.valueOf(input.getRuleType()));
        rule.setAmountThreshold(input.getAmountThreshold());
        rule.setMerchantCategory(input.getMerchantCategory() != null ?
                MerchantCategory.valueOf(input.getMerchantCategory()) : null);
        rule.setFrequencyCount(input.getFrequencyCount());
        rule.setFrequencyWindowMinutes(input.getFrequencyWindowMinutes());
        rule.setRiskPoints(input.getRiskPoints());
        rule.setActive(input.getActive());

        RiskRule updatedRule = riskRuleRepository.save(rule);
        return toDTO(updatedRule);
    }

    private void validateRuleInput(RiskRuleInput input) {
        RuleType ruleType;
        try {
            ruleType = RuleType.valueOf(input.getRuleType());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid rule type: " + input.getRuleType());
        }

        switch (ruleType) {
            case AMOUNT_THRESHOLD:
                if (input.getAmountThreshold() == null) {
                    throw new BadRequestException("Amount threshold is required for AMOUNT_THRESHOLD rule");
                }
                break;
            case MERCHANT_CATEGORY:
                if (input.getMerchantCategory() == null) {
                    throw new BadRequestException("Merchant category is required for MERCHANT_CATEGORY rule");
                }
                try {
                    MerchantCategory.valueOf(input.getMerchantCategory());
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Invalid merchant category: " + input.getMerchantCategory());
                }
                break;
            case FREQUENCY:
                if (input.getFrequencyCount() == null || input.getFrequencyWindowMinutes() == null) {
                    throw new BadRequestException("Frequency count and window minutes are required for FREQUENCY rule");
                }
                break;
        }
    }

    private RiskRuleDTO toDTO(RiskRule rule) {
        return RiskRuleDTO.builder()
                .id(rule.getId())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType().name())
                .amountThreshold(rule.getAmountThreshold())
                .merchantCategory(rule.getMerchantCategory() != null ? rule.getMerchantCategory().name() : null)
                .frequencyCount(rule.getFrequencyCount())
                .frequencyWindowMinutes(rule.getFrequencyWindowMinutes())
                .riskPoints(rule.getRiskPoints())
                .active(rule.getActive())
                .build();
    }
}

