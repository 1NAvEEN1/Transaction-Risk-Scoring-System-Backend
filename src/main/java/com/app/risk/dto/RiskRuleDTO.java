package com.app.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRuleDTO {
    private Long id;
    private String ruleName;
    private String ruleType;
    private BigDecimal amountThreshold;
    private String merchantCategory;
    private Integer frequencyCount;
    private Integer frequencyWindowMinutes;
    private Integer riskPoints;
    private Boolean active;
}

