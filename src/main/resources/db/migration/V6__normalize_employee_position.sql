-- ============================================================
-- MotorX - Migración V6: Normalización del cargo (position) de empleados
-- Proyecto: Spark Tech S.A.S
-- Motivo: El campo position pasa de VARCHAR libre a un enum controlado
--         con solo dos valores válidos: RECEPCIONISTA y MECANICO.
-- ============================================================

-- 1. Estandarizar valores previos que puedan existir en la columna
--    (limpieza defensiva antes de aplicar el CHECK constraint)
UPDATE employees
SET position = 'RECEPCIONISTA'
WHERE UPPER(TRIM(position)) IN ('RECEPCIONISTA', 'RECEPTIONIST', 'RECEPCION', 'RECEPCIONISTA ');

UPDATE employees
SET position = 'MECANICO'
WHERE UPPER(TRIM(position)) IN ('MECANICO', 'MECHANIC', 'MECÁNICO', 'TÉCNICO', 'TECNICO');

-- 2. Ajustar la longitud de la columna a 50 caracteres (suficiente para los valores del enum)
ALTER TABLE employees
    ALTER COLUMN position TYPE VARCHAR(50);

-- 3. Agregar CHECK constraint que restringe los valores a los del enum EmployeePosition
ALTER TABLE employees
    ADD CONSTRAINT chk_employees_position
        CHECK (position IN ('RECEPCIONISTA', 'MECANICO'));

-- 4. Crear índice sobre position para acelerar las consultas de asignación de citas
--    (findAllActive filtra WHERE position = 'MECANICO' AND state = 'AVAILABLE')
CREATE INDEX idx_employee_position ON employees (position);

