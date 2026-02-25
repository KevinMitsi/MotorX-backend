-- ============================================================
-- MotorX - Migración V5: Agregar año de fabricación a vehículos
--          y kilometraje actual a citas
-- Proyecto: Spark Tech S.A.S
-- ============================================================

-- 1. Agregar columna year_of_manufacture a la tabla vehicles
ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS year_of_manufacture INT NULL;

-- Rellenar con un valor temporal para filas existentes
UPDATE vehicles SET year_of_manufacture = 2000 WHERE year_of_manufacture IS NULL;

-- Hacer la columna NOT NULL
ALTER TABLE vehicles ALTER COLUMN year_of_manufacture SET NOT NULL;

-- Agregar restricción de dominio (1950 - 2026)
ALTER TABLE vehicles
    ADD CONSTRAINT chk_vehicles_year_of_manufacture
        CHECK (year_of_manufacture >= 1950 AND year_of_manufacture <= 2026);


-- 2. Agregar columna current_mileage a la tabla appointments
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS current_mileage INT NULL;

-- Rellenar con un valor temporal para filas existentes
UPDATE appointments SET current_mileage = 0 WHERE current_mileage IS NULL;

-- Hacer la columna NOT NULL
ALTER TABLE appointments ALTER COLUMN current_mileage SET NOT NULL;

-- Agregar restricción de dominio (>= 0)
ALTER TABLE appointments
    ADD CONSTRAINT chk_appointments_current_mileage
        CHECK (current_mileage >= 0);

