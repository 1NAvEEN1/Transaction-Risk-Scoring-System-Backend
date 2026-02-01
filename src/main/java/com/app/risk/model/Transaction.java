package com.app.risk.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_customer", columnList = "customer_id"),
    @Index(name = "idx_transaction_timestamp", columnList = "timestamp"),
    @Index(name = "idx_transaction_merchant_category", columnList = "merchantCategory"),
    @Index(name = "idx_transaction_status", columnList = "status"),
    @Index(name = "idx_transaction_customer_timestamp", columnList = "customer_id, timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull
    private Customer customer;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(nullable = false)
    private String currency;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private MerchantCategory merchantCategory;

    @NotNull
    @Column(nullable = false)
    private Integer riskScore;

    @Column(columnDefinition = "TEXT")
    private String matchedRulesJson;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private TransactionStatus status;
}

