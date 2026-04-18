package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.Spare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaSpareRepository extends JpaRepository<Spare, Long> {
    boolean existsBySavCode(String savCode);

    boolean existsBySpareCode(String spareCode);

    boolean existsBySavCodeAndIdNot(String savCode, Long id);

    boolean existsBySpareCodeAndIdNot(String spareCode, Long id);

    @Query("SELECT s FROM Spare s WHERE s.stockThreshold > 0 AND s.quantity < s.stockThreshold")
    List<Spare> findLowStockSpares();

    @Query("""
            SELECT s FROM Spare s
            WHERE (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:savCode IS NULL OR LOWER(s.savCode) LIKE LOWER(CONCAT('%', :savCode, '%')))
            ORDER BY s.name ASC
            """)
    List<Spare> searchByNameAndSavCode(@Param("name") String name, @Param("savCode") String savCode);
}

