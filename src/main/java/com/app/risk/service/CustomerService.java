package com.app.risk.service;

import com.app.risk.audit.AuditLogService;
import com.app.risk.exception.NotFoundException;
import com.app.risk.entity.Customer;
import com.app.risk.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    public Customer findById(Long id) {
        log.debug("Finding customer by id: {}", id);

        return customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Customer not found with id: {}", id);
                    auditLogService.logError("FIND_CUSTOMER", "Customer", id,
                            "Customer not found", null);
                    return new NotFoundException("Customer not found with id: " + id);
                });
    }

    public List<Customer> findAll() {
        log.debug("Retrieving all customers");

        List<Customer> customers = customerRepository.findAll();

        log.info("Retrieved {} customers", customers.size());

        Map<String, Object> details = new HashMap<>();
        details.put("count", customers.size());

        auditLogService.logCustomEvent(
                "CUSTOMERS_RETRIEVED",
                "LIST_CUSTOMERS",
                "Customer",
                null,
                "SUCCESS",
                details
        );

        return customers;
    }
}


