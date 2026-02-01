package com.app.risk.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents an audit log entry for tracking system activities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    private LocalDateTime timestamp;
    private String eventType;
    private String action;
    private String userId;
    private String userName;
    private String resource;
    private Long resourceId;
    private String status;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> details;
    private String errorMessage;
    private Long executionTimeMs;

    public enum EventType {
        TRANSACTION_SUBMITTED,
        TRANSACTION_FLAGGED,
        TRANSACTION_APPROVED,
        TRANSACTION_RETRIEVED,
        CUSTOMER_CREATED,
        CUSTOMER_UPDATED,
        CUSTOMER_RETRIEVED,
        RISK_RULE_CREATED,
        RISK_RULE_UPDATED,
        RISK_RULE_DELETED,
        RISK_RULE_ACTIVATED,
        RISK_RULE_DEACTIVATED,
        RISK_EVALUATION_PERFORMED,
        AUTHENTICATION_SUCCESS,
        AUTHENTICATION_FAILURE,
        AUTHORIZATION_FAILURE,
        SYSTEM_ERROR
    }

    public enum Status {
        SUCCESS,
        FAILURE,
        WARNING
    }
}

