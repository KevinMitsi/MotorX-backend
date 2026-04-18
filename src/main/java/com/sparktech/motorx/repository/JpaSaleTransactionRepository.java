package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.SaleTransaction;
import com.sparktech.motorx.entity.SaleTransactionItem;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT i.spare.id, i.spare.name, i.spare.savCode, SUM(i.quantity)
            FROM SaleTransaction s
            JOIN s.items i
            GROUP BY i.spare.id, i.spare.name, i.spare.savCode
            ORDER BY SUM(i.quantity) DESC
            """)
    List<Object[]> findTopSellingSpares(Pageable pageable);

    @Query("""
            SELECT i FROM SaleTransaction s
            JOIN s.items i
            JOIN FETCH i.spare sp
            WHERE s.transactionDate >= :start AND s.transactionDate < :end
            """)
    List<SaleTransactionItem> findSoldItemsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT i.spare.id, MAX(s.transactionDate)
            FROM SaleTransaction s
            JOIN s.items i
            GROUP BY i.spare.id
            """)
    List<Object[]> findLastSaleDatePerSpare();
}

