-- ============================================================
-- MotorX - Migración V4: Agregar número de chasis a vehículos
-- El número de chasis es único tal como aparece en la
-- tarjeta de propiedad del vehículo (Colombia).
-- ============================================================

ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS chassis_number VARCHAR(50) NULL;

-- Rellenar temporal para filas existentes (si las hay)
UPDATE vehicles SET chassis_number = CONCAT('TEMP-', id) WHERE chassis_number IS NULL;

-- Hacer la columna NOT NULL y UNIQUE
ALTER TABLE vehicles ALTER COLUMN chassis_number SET NOT NULL;
ALTER TABLE vehicles ADD CONSTRAINT uq_vehicles_chassis_number UNIQUE (chassis_number);

CREATE INDEX IF NOT EXISTS idx_vehicle_chassis ON vehicles (chassis_number);

