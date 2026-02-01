package com.app.risk.repository;

import com.app.risk.model.Transaction;
import com.app.risk.model.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    long countByCustomerIdAndTimestampAfter(Long customerId, LocalDateTime cutoffTime);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:searchQuery IS NULL OR :searchQuery = '' OR " +
           "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
           "LOWER(t.customer.email) LIKE LOWER(CONCAT('%', :searchQuery, '%')))")
    Page<Transaction> findByStatusAndCustomerSearch(
        @Param("status") TransactionStatus status,
        @Param("searchQuery") String searchQuery,
        Pageable pageable
    );
}

