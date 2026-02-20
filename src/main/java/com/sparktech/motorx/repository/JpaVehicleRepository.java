package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaVehicleRepository extends JpaRepository<VehicleEntity, Long> {

    // --- Búsqueda básica ---
    Optional<VehicleEntity> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    // --- Vehículos por propietario ---
    // Necesario para el historial de citas del cliente (Proceso 2)
    List<VehicleEntity> findByOwner(UserEntity owner);

    List<VehicleEntity> findByOwnerId(Long ownerId);

    // --- Soporte para historial de servicios del cliente (Proceso 1 - salida: historial) ---
    @Query("""
            SELECT v FROM VehicleEntity v
            WHERE v.owner.id = :ownerId
            ORDER BY v.createdAt DESC
            """)
    List<VehicleEntity> findByOwnerIdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);

    // --- Búsqueda por marca y modelo (soporte administrativo) ---
    List<VehicleEntity> findByBrandIgnoreCaseAndModelIgnoreCase(String brand, String model);
}
