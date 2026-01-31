package com.app.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchedRule {
    private Long ruleId;
    private String ruleName;
    private String ruleType;
    private Integer points;
    private String reason;
}

