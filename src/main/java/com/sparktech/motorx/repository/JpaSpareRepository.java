package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.Spare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaSpareRepository extends JpaRepository<Spare, Long> {
    boolean existsBySavCode(String savCode);

    boolean existsBySpareCode(String spareCode);

    boolean existsBySavCodeAndIdNot(String savCode, Long id);

    boolean existsBySpareCodeAndIdNot(String spareCode, Long id);
}

