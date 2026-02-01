package com.app.risk.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for logging audit trail events
 * Provides structured logging for security, compliance, and debugging purposes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final ObjectMapper objectMapper;

    /**
     * Log a transaction submission event
     */
    public void logTransactionSubmitted(Long transactionId, Long customerId, String customerEmail,
                                       Map<String, Object> details) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(AuditLog.EventType.TRANSACTION_SUBMITTED.name())
                .action("SUBMIT_TRANSACTION")
                .resource("Transaction")
                .resourceId(transactionId)
                .userId(customerId.toString())
                .userName(customerEmail)
                .status(AuditLog.Status.SUCCESS.name())
                .details(details)
                .build();

        logAudit(auditLog);
    }

    /**
     * Log when a transaction is flagged for review
     */
    public void logTransactionFlagged(Long transactionId, Long customerId, int riskScore,
                                     int matchedRulesCount, Map<String, Object> details) {
        Map<String, Object> enrichedDetails = new HashMap<>(details != null ? details : new HashMap<>());
        enrichedDetails.put("riskScore", riskScore);
        enrichedDetails.put("matchedRulesCount", matchedRulesCount);

        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(AuditLog.EventType.TRANSACTION_FLAGGED.name())
                .action("FLAG_TRANSACTION")
                .resource("Transaction")
                .resourceId(transactionId)
                .userId(customerId.toString())
                .status(AuditLog.Status.WARNING.name())
                .details(enrichedDetails)
                .build();

        logAuditWarning(auditLog);
    }

    /**
     * Log when a transaction is approved
     */
    public void logTransactionApproved(Long transactionId, Long customerId, int riskScore) {
        Map<String, Object> details = new HashMap<>();
        details.put("riskScore", riskScore);

        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(AuditLog.EventType.TRANSACTION_APPROVED.name())
                .action("APPROVE_TRANSACTION")
                .resource("Transaction")
                .resourceId(transactionId)
                .userId(customerId.toString())
                .status(AuditLog.Status.SUCCESS.name())
                .details(details)
                .build();

        logAudit(auditLog);
    }

    /**
     * Log risk evaluation performed
     */
    public void logRiskEvaluation(Long transactionId, Long customerId, int totalRiskScore,
                                 int rulesEvaluated, int rulesMatched, long executionTimeMs) {
        Map<String, Object> details = new HashMap<>();
        details.put("totalRiskScore", totalRiskScore);
        details.put("rulesEvaluated", rulesEvaluated);
        details.put("rulesMatched", rulesMatched);

        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(AuditLog.EventType.RISK_EVALUATION_PERFORMED.name())
                .action("EVALUATE_RISK")
                .resource("Transaction")
                .resourceId(transactionId)
                .userId(customerId.toString())
                .status(AuditLog.Status.SUCCESS.name())
                .details(details)
                .executionTimeMs(executionTimeMs)
                .build();

        logAudit(auditLog);
    }

    /**
     * Log customer creation
     */
    public void logCustomerCreated(Long customerId, String email, String riskProfile) {
        Map<String, Object> details = new HashMap<>();
        details.put("email", email);
        details.put("riskProfile", riskProfile);

        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(AuditLog.EventType.CUSTOMER_CREATED.name())
                .action("CREATE_CUSTOMER")
                .resource("Customer")
                .resourceId(customerId)
                .status(AuditLog.Status.SUCCESS.name())
                .details(details)
                .build();

        logAudit(auditLog);
    }

    /**
     * Log risk rule creation
     */
    public void logRiskRuleCreated(Long ruleId, String ruleName, String ruleType, int riskPoints) {
        Map<String, Object> details = new HashMap<>();
        details.put("ruleName", ruleName);
        details.put("ruleType", ruleType);
        details.put("riskPoints", riskPoints);

        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(AuditLog.EventType.RISK_RULE_CREATED.name())
                .action("CREATE_RISK_RULE")
                .resource("RiskRule")
                .resourceId(ruleId)
                .status(AuditLog.Status.SUCCESS.name())
                .details(details)
                .build();

        logAudit(auditLog);
    }

    /**
     * Log risk rule update
     */
    public void logRiskRuleUpdated(Long ruleId, String ruleName, Map<String, Object> changes) {
        Map<String, Object> details = new HashMap<>();
        details.put("ruleName", ruleName);
        details.put("changes", changes);

        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(AuditLog.EventType.RISK_RULE_UPDATED.name())
                .action("UPDATE_RISK_RULE")
                .resource("RiskRule")
                .resourceId(ruleId)
                .status(AuditLog.Status.SUCCESS.name())
                .details(details)
                .build();

        logAudit(auditLog);
    }

    /**
     * Log risk rule deletion
     */
    public void logRiskRuleDeleted(Long ruleId, String ruleName) {
        Map<String, Object> details = new HashMap<>();
        details.put("ruleName", ruleName);

        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(AuditLog.EventType.RISK_RULE_DELETED.name())
                .action("DELETE_RISK_RULE")
                .resource("RiskRule")
                .resourceId(ruleId)
                .status(AuditLog.Status.SUCCESS.name())
                .details(details)
                .build();

        logAudit(auditLog);
    }

    /**
     * Log system errors
     */
    public void logError(String action, String resource, Long resourceId, String errorMessage,
                        Exception exception) {
        Map<String, Object> details = new HashMap<>();
        if (exception != null) {
            details.put("exceptionType", exception.getClass().getName());
            details.put("stackTrace", getStackTraceString(exception));
        }

        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(AuditLog.EventType.SYSTEM_ERROR.name())
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .status(AuditLog.Status.FAILURE.name())
                .errorMessage(errorMessage)
                .details(details)
                .build();

        logAuditError(auditLog);
    }

    /**
     * Generic method to log any custom audit event
     */
    public void logCustomEvent(String eventType, String action, String resource, Long resourceId,
                              String status, Map<String, Object> details) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .status(status)
                .details(details)
                .build();

        logAudit(auditLog);
    }

    /**
     * Internal method to log audit entries as JSON
     */
    private void logAudit(AuditLog auditLog) {
        try {
            String jsonLog = objectMapper.writeValueAsString(auditLog);
            log.info("AUDIT: {}", jsonLog);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log", e);
            log.info("AUDIT: eventType={}, action={}, resource={}, resourceId={}, status={}",
                    auditLog.getEventType(), auditLog.getAction(), auditLog.getResource(),
                    auditLog.getResourceId(), auditLog.getStatus());
        }
    }

    /**
     * Log audit warnings
     */
    private void logAuditWarning(AuditLog auditLog) {
        try {
            String jsonLog = objectMapper.writeValueAsString(auditLog);
            log.warn("AUDIT_WARNING: {}", jsonLog);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log", e);
            log.warn("AUDIT_WARNING: eventType={}, action={}, resource={}, resourceId={}, status={}",
                    auditLog.getEventType(), auditLog.getAction(), auditLog.getResource(),
                    auditLog.getResourceId(), auditLog.getStatus());
        }
    }

    /**
     * Log audit errors
     */
    private void logAuditError(AuditLog auditLog) {
        try {
            String jsonLog = objectMapper.writeValueAsString(auditLog);
            log.error("AUDIT_ERROR: {}", jsonLog);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log", e);
            log.error("AUDIT_ERROR: eventType={}, action={}, resource={}, resourceId={}, status={}, error={}",
                    auditLog.getEventType(), auditLog.getAction(), auditLog.getResource(),
                    auditLog.getResourceId(), auditLog.getStatus(), auditLog.getErrorMessage());
        }
    }

    /**
     * Helper method to get stack trace as string
     */
    private String getStackTraceString(Exception e) {
        if (e == null) return null;
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            if (sb.length() > 500) break; // Limit stack trace size
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}

