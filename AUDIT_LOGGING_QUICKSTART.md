# Audit Logging - Quick Start Guide

## 🚀 Getting Started

The audit logging system is **already integrated and working**. No additional configuration is needed!

## ✅ What's Already Logging

### Automatic Audit Trails
When you use the system, the following are automatically logged:

1. **Transaction Submissions**
   ```java
   POST /graphql
   mutation {
     submitTransaction(input: {...}) {
       id
       riskScore
       status
     }
   }
   ```
   **Logs:**
   - Transaction details (amount, merchant, customer)
   - Risk score calculation
   - Matched rules
   - Execution time
   - FLAGGED or APPROVED status

2. **Transaction Retrievals**
   ```java
   GET /api/transactions/{id}
   ```
   **Logs:**
   - Which transaction was accessed
   - Customer information

3. **Risk Rule Changes**
   ```java
   POST /graphql - createRiskRule
   PUT /graphql - updateRiskRule
   ```
   **Logs:**
   - Rule creation with all parameters
   - Rule updates with before/after changes

## 📁 Where to Find Logs

### Application Logs
```
Location: logs/application.log
Contains: All application activity (DEBUG, INFO, WARN, ERROR)
```

### Audit Logs
```
Location: logs/audit.log
Contains: Structured JSON audit events only
Format: One JSON object per line
```

## 🔍 Quick Log Queries

### View Real-time Logs (Windows)
```cmd
# Watch application log
Get-Content logs\application.log -Wait -Tail 50

# Watch audit log
Get-Content logs\audit.log -Wait -Tail 20
```

### Find Flagged Transactions
```cmd
findstr "FLAGGED" logs\application.log
findstr "TRANSACTION_FLAGGED" logs\audit.log
```

### View Today's Activity
```cmd
findstr "%date:~-4,4%-%date:~-7,2%-%date:~-10,2%" logs\application.log
```

### Count Event Types
```powershell
Get-Content logs\audit.log | Select-String '"eventType":"[^"]*"' -AllMatches | ForEach-Object { $_.Matches.Value } | Group-Object | Sort-Object Count -Descending
```

## 📊 Example Log Output

### Transaction Submitted (Approved)
```
2026-02-02 10:15:30.123 [http-nio-8080-exec-1] INFO  TransactionService - Processing transaction submission for customer: 1
2026-02-02 10:15:30.145 [http-nio-8080-exec-1] INFO  TransactionService - Transaction risk evaluation complete. Score: 40, Status: APPROVED, Matched rules: 1
2026-02-02 10:15:30.167 [http-nio-8080-exec-1] INFO  TransactionService - Transaction 123 processed successfully in 44ms
```

### Transaction Flagged (High Risk)
```
2026-02-02 10:20:15.234 [http-nio-8080-exec-2] INFO  TransactionService - Processing transaction submission for customer: 2
2026-02-02 10:20:15.301 [http-nio-8080-exec-2] INFO  TransactionService - Transaction risk evaluation complete. Score: 90, Status: FLAGGED, Matched rules: 2
2026-02-02 10:20:15.323 [http-nio-8080-exec-2] WARN  TransactionService - Transaction 124 flagged for review. Customer: customer@example.com, Score: 90
2026-02-02 10:20:15.345 [http-nio-8080-exec-2] INFO  TransactionService - Transaction 124 processed successfully in 111ms
```

### Audit Trail (JSON)
```json
2026-02-02 10:20:15.324 - {"timestamp":"2026-02-02T10:20:15.324","eventType":"TRANSACTION_FLAGGED","action":"FLAG_TRANSACTION","resource":"Transaction","resourceId":124,"userId":"2","status":"WARNING","details":{"amount":15000.00,"merchantCategory":"GAMBLING","riskScore":90,"matchedRulesCount":2,"matchedRules":["High Amount","Gambling"]}}
```

## 🎯 What Gets Logged

### For Every Transaction:
- ✅ Customer ID and email
- ✅ Transaction amount and currency
- ✅ Merchant category
- ✅ Timestamp
- ✅ Calculated risk score
- ✅ Status (APPROVED/FLAGGED)
- ✅ Matched risk rules
- ✅ Execution time (performance)

### For Flagged Transactions (Risk Score ≥ 70):
- ⚠️ **WARNING level log** for visibility
- ⚠️ List of all matched rule names
- ⚠️ Individual rule contributions
- ⚠️ Customer information for follow-up

### For System Errors:
- ❌ Error messages
- ❌ Exception details
- ❌ Stack traces (limited)
- ❌ Context information

## 🔧 Common Tasks

### Monitor Flagged Transactions
```cmd
# Real-time monitoring
Get-Content logs\application.log -Wait | Select-String "flagged for review"
```

### Daily Report
```powershell
# Count transactions by status today
$today = Get-Date -Format "yyyy-MM-dd"
Get-Content logs\application.log | Select-String $today | Select-String "Status: (APPROVED|FLAGGED)" -AllMatches | ForEach-Object { $_.Matches.Value } | Group-Object
```

### Find High-Risk Customers
```cmd
# Customers with multiple flagged transactions
findstr "flagged for review" logs\application.log | findstr /R "Customer: [^,]*" 
```

### Performance Analysis
```cmd
# Find slow transactions (>100ms)
findstr "processed successfully" logs\application.log | findstr /R "[0-9][0-9][0-9]ms"
```

## 🔒 Security Notes

1. **Log Files are Sensitive**
   - Contains customer information
   - Protect access to logs directory
   - Consider encryption for production

2. **Retention Policy**
   - Application logs: 30 days
   - Audit logs: 90 days (compliance)
   - Automatically rotated daily

3. **GDPR Compliance**
   - Logs contain personal data (email addresses)
   - Include in data deletion requests
   - Document in privacy policy

## 📈 Performance

- **Logging Overhead:** 5-10ms per transaction
- **File I/O:** Asynchronous (non-blocking)
- **Disk Space:** ~10MB per day (estimated)
- **Compression:** Old logs can be compressed

## 🎓 Next Steps

1. **Review Logs Regularly**
   - Check for flagged transactions daily
   - Monitor error rates
   - Track performance trends

2. **Set Up Alerts** (Optional)
   - Email on high-risk transactions
   - Slack notifications for system errors
   - Daily summary reports

3. **Archive Old Logs** (Recommended)
   - Move old logs to long-term storage
   - Compress logs older than 30 days
   - Backup logs regularly

4. **Advanced Analysis** (Future)
   - Import logs into analytics tools
   - Create dashboards (Grafana, Kibana)
   - Machine learning on patterns

## 📚 Full Documentation

For detailed information, see:
- **AUDIT_LOGGING_GUIDE.md** - Complete guide with examples
- **AUDIT_LOGGING_SUMMARY.md** - Implementation summary

## ❓ FAQ

**Q: Where are the log files?**  
A: In the `logs/` directory in your project root.

**Q: How do I disable audit logging?**  
A: Change log level in `application-dev.yml`: `com.app.risk.audit: OFF`

**Q: Can I change the log format?**  
A: Yes, edit `logback-spring.xml` to customize patterns.

**Q: How long are logs kept?**  
A: Application logs: 30 days, Audit logs: 90 days (auto-rotated).

**Q: Do logs affect performance?**  
A: Minimal impact (~5-10ms per transaction), asynchronous logging.

**Q: Are logs secure?**  
A: File-based, access controlled by OS permissions. Consider encryption for production.

---

**Ready to Use!** The audit logging is already working. Just submit transactions and check the logs! 🎉

