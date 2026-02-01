package com.app.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
    private String merchantCategory;
    private Integer riskScore;
    private String status;
    private List<MatchedRule> matchedRules;
}

