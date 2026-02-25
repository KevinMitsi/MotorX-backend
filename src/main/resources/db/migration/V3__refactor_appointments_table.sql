-- ============================================================
-- MotorX - Migración V3: Refactorización de la tabla appointments
-- Se elimina la FK a services (service_id) y description,
-- y se agrega: appointment_type, technician_id, client_notes,
-- admin_notes, cancellation_reason.
-- ============================================================

-- 1. Eliminar índice y FK de service_id
DROP INDEX IF EXISTS idx_appointment_service;
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS fk_appointments_service;

-- 2. Eliminar columnas que ya no aplican
ALTER TABLE appointments DROP COLUMN IF EXISTS service_id;
ALTER TABLE appointments DROP COLUMN IF EXISTS description;

-- 3. Agregar nuevas columnas
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS appointment_type    VARCHAR(30)  NOT NULL DEFAULT 'QUICK_SERVICE',
    ADD COLUMN IF NOT EXISTS technician_id       BIGINT       NULL,
    ADD COLUMN IF NOT EXISTS client_notes        VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS admin_notes         VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500) NULL;

-- 4. Agregar FK al técnico (empleado)
ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_technician
        FOREIGN KEY (technician_id) REFERENCES employees (id)
            ON DELETE SET NULL ON UPDATE CASCADE;

-- 5. Agregar CHECK constraint para appointment_type
ALTER TABLE appointments
    ADD CONSTRAINT chk_appointments_type CHECK (
        appointment_type IN (
            'MANUAL_WARRANTY_REVIEW',
            'AUTECO_WARRANTY',
            'QUICK_SERVICE',
            'MAINTENANCE',
            'OIL_CHANGE',
            'UNPLANNED',
            'REWORK'
        )
    );

-- 6. Actualizar índices
DROP INDEX IF EXISTS idx_appointment_state;
CREATE INDEX IF NOT EXISTS idx_appointment_status     ON appointments (status);
CREATE INDEX IF NOT EXISTS idx_appointment_technician ON appointments (technician_id);

-- 7. Quitar DEFAULT temporal de appointment_type (opcional, ya que es NOT NULL y los registros existentes lo tienen)
ALTER TABLE appointments ALTER COLUMN appointment_type DROP DEFAULT;

