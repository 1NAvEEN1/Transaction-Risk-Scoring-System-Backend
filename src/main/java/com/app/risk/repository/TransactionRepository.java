package com.app.risk.repository;

import com.app.risk.model.Transaction;
import com.app.risk.model.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    long countByCustomerIdAndTimestampAfter(Long customerId, LocalDateTime cutoffTime);
}

