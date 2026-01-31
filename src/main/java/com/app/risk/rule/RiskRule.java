package com.app.risk.rule;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "risk_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private RuleType ruleType;

    // For AMOUNT_THRESHOLD rules
    @Column(precision = 19, scale = 2)
    private BigDecimal amountThreshold;

    // For MERCHANT_CATEGORY rules
    @Enumerated(EnumType.STRING)
    private com.app.risk.transaction.MerchantCategory merchantCategory;

    // For FREQUENCY rules
    private Integer frequencyCount;
    private Integer frequencyWindowMinutes;

    @NotNull
    @Column(nullable = false)
    private Integer riskPoints;

    @NotNull
    @Column(nullable = false)
    private Boolean active;
}

