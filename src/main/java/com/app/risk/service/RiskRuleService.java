package com.app.risk.service;

import com.app.risk.audit.AuditLogService;
import com.app.risk.dto.RiskRuleDTO;
import com.app.risk.dto.RiskRuleInput;
import com.app.risk.exception.BadRequestException;
import com.app.risk.exception.NotFoundException;
import com.app.risk.entity.MerchantCategory;
import com.app.risk.entity.RiskRule;
import com.app.risk.entity.RuleType;
import com.app.risk.repository.RiskRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskRuleService {

    private final RiskRuleRepository riskRuleRepository;
    private final AuditLogService auditLogService;

    public List<RiskRuleDTO> getAllRules() {
        log.debug("Retrieving all risk rules");

        List<RiskRuleDTO> rules = riskRuleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        log.info("Retrieved {} risk rules", rules.size());

        return rules;
    }

    public List<RiskRule> getActiveRules() {
        log.debug("Retrieving active risk rules");

        List<RiskRule> activeRules = riskRuleRepository.findByActiveTrue();

        log.info("Retrieved {} active risk rules", activeRules.size());

        return activeRules;
    }

    @Transactional
    public RiskRuleDTO createRule(RiskRuleInput input) {
        log.info("Creating new risk rule: {}", input.getRuleName());

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

        // Audit logging
        auditLogService.logRiskRuleCreated(
                savedRule.getId(),
                savedRule.getRuleName(),
                savedRule.getRuleType().name(),
                savedRule.getRiskPoints()
        );

        log.info("Risk rule created successfully with id: {}", savedRule.getId());

        return toDTO(savedRule);
    }

    @Transactional
    public RiskRuleDTO updateRule(Long id, RiskRuleInput input) {
        log.info("Updating risk rule with id: {}", id);

        RiskRule rule = riskRuleRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Risk rule not found with id: {}", id);
                    return new NotFoundException("Risk rule not found with id: " + id);
                });

        validateRuleInput(input);

        // Track changes for audit
        Map<String, Object> changes = new HashMap<>();
        if (!rule.getRuleName().equals(input.getRuleName())) {
            changes.put("ruleName", Map.of("old", rule.getRuleName(), "new", input.getRuleName()));
        }
        if (rule.getRiskPoints() != input.getRiskPoints()) {
            changes.put("riskPoints", Map.of("old", rule.getRiskPoints(), "new", input.getRiskPoints()));
        }
        if (rule.getActive() != input.getActive()) {
            changes.put("active", Map.of("old", rule.getActive(), "new", input.getActive()));
        }

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

        // Audit logging
        auditLogService.logRiskRuleUpdated(updatedRule.getId(), updatedRule.getRuleName(), changes);

        log.info("Risk rule updated successfully: {}", updatedRule.getId());

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

