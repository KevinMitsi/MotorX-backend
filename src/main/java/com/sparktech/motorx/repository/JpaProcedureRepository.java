package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.ProcedureEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaProcedureRepository extends JpaRepository<@NotNull ProcedureEntity, @NotNull Long> {

    Optional<ProcedureEntity> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<ProcedureEntity> findByActiveTrueOrderByNameAsc();
}


