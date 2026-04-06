package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.SaleTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JpaSaleTransactionRepository extends JpaRepository<SaleTransaction, Long> {
    List<SaleTransaction> findAllByOrderByTransactionDateDesc();

    @Query("""
            SELECT s FROM SaleTransaction s
            WHERE s.transactionDate >= :start AND s.transactionDate < :end
            ORDER BY s.transactionDate DESC
            """)
    List<SaleTransaction> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

