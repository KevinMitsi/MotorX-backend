package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.PurchaseTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaPurchaseTransactionRepository extends JpaRepository<PurchaseTransaction, Long> {
    List<PurchaseTransaction> findAllByOrderByTransactionDateDesc();
}

