package com.app.risk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRuleInput {

    @NotBlank
    private String ruleName;

    @NotBlank
    private String ruleType;

    private BigDecimal amountThreshold;

    private String merchantCategory;

    private Integer frequencyCount;

    private Integer frequencyWindowMinutes;

    @NotNull
    private Integer riskPoints;

    @NotNull
    private Boolean active;
}

