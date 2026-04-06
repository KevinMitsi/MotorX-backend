-- ============================================================
-- MotorX - Migracion V9: Modulo de inventario y flujo de recepcion
-- ============================================================

-- 1) Ampliar posiciones de empleados
ALTER TABLE employees DROP CONSTRAINT IF EXISTS chk_employees_position;
ALTER TABLE employees
    ADD CONSTRAINT chk_employees_position
        CHECK (position IN ('RECEPCIONISTA', 'MECANICO', 'WAREHOUSE_WORKER'));

-- 2) Extender estados de cita + codigo temporal de recepcion
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS chk_appointments_status;
ALTER TABLE appointments
    ADD CONSTRAINT chk_appointments_status CHECK (
        status IN ('SCHEDULED','AWAITING_CONFIRMATION','IN_PROGRESS','COMPLETED','CANCELLED','REJECTED','NO_SHOW')
    );

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS verification_code VARCHAR(4) NULL,
    ADD COLUMN IF NOT EXISTS verification_code_created_at TIMESTAMP NULL;

-- 3) Catalogo de repuestos
CREATE TABLE spares (
    id BIGSERIAL NOT NULL,
    name VARCHAR(150) NOT NULL,
    compatible_motorcycles VARCHAR(500) NOT NULL,
    sav_code VARCHAR(80) NOT NULL,
    spare_code VARCHAR(80) NOT NULL,
    purchase_price_with_vat DECIMAL(12, 2) NOT NULL,
    is_oil BOOLEAN NOT NULL DEFAULT FALSE,
    supplier VARCHAR(150) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    warehouse_location VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_spares PRIMARY KEY (id),
    CONSTRAINT uq_spares_sav_code UNIQUE (sav_code),
    CONSTRAINT uq_spares_spare_code UNIQUE (spare_code),
    CONSTRAINT chk_spares_purchase_price CHECK (purchase_price_with_vat >= 0),
    CONSTRAINT chk_spares_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_spares_location_format CHECK (warehouse_location ~ '^[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}$')
);

CREATE INDEX IF NOT EXISTS idx_spares_name ON spares (name);
CREATE INDEX IF NOT EXISTS idx_spares_supplier ON spares (supplier);
CREATE INDEX IF NOT EXISTS idx_spares_quantity ON spares (quantity);

-- 4) Entradas de inventario
CREATE TABLE purchase_transactions (
    id BIGSERIAL NOT NULL,
    supplier VARCHAR(150) NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    created_by_user_id BIGINT NOT NULL,

    CONSTRAINT pk_purchase_transactions PRIMARY KEY (id),
    CONSTRAINT fk_purchase_transactions_user FOREIGN KEY (created_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_purchase_tx_date ON purchase_transactions (transaction_date);
CREATE INDEX IF NOT EXISTS idx_purchase_tx_supplier ON purchase_transactions (supplier);

CREATE TABLE purchase_transaction_items (
    id BIGSERIAL NOT NULL,
    purchase_transaction_id BIGINT NOT NULL,
    spare_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    purchase_price_with_vat DECIMAL(12,2) NOT NULL,

    CONSTRAINT pk_purchase_transaction_items PRIMARY KEY (id),
    CONSTRAINT fk_purchase_item_transaction FOREIGN KEY (purchase_transaction_id) REFERENCES purchase_transactions (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_purchase_item_spare FOREIGN KEY (spare_id) REFERENCES spares (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_purchase_item_qty CHECK (quantity > 0),
    CONSTRAINT chk_purchase_item_price CHECK (purchase_price_with_vat >= 0)
);

CREATE INDEX IF NOT EXISTS idx_purchase_item_tx ON purchase_transaction_items (purchase_transaction_id);
CREATE INDEX IF NOT EXISTS idx_purchase_item_spare ON purchase_transaction_items (spare_id);

-- 5) Salidas de inventario
CREATE TABLE sale_transactions (
    id BIGSERIAL NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    appointment_id BIGINT NULL,
    created_by_user_id BIGINT NOT NULL,

    CONSTRAINT pk_sale_transactions PRIMARY KEY (id),
    CONSTRAINT fk_sale_transactions_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_sale_transactions_user FOREIGN KEY (created_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sale_tx_date ON sale_transactions (transaction_date);
CREATE INDEX IF NOT EXISTS idx_sale_tx_appointment ON sale_transactions (appointment_id);

CREATE TABLE sale_transaction_items (
    id BIGSERIAL NOT NULL,
    sale_transaction_id BIGINT NOT NULL,
    spare_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    sale_price_at_moment DECIMAL(12,2) NOT NULL,

    CONSTRAINT pk_sale_transaction_items PRIMARY KEY (id),
    CONSTRAINT fk_sale_item_transaction FOREIGN KEY (sale_transaction_id) REFERENCES sale_transactions (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_sale_item_spare FOREIGN KEY (spare_id) REFERENCES spares (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_sale_item_qty CHECK (quantity > 0),
    CONSTRAINT chk_sale_item_price CHECK (sale_price_at_moment >= 0)
);

CREATE INDEX IF NOT EXISTS idx_sale_item_tx ON sale_transaction_items (sale_transaction_id);
CREATE INDEX IF NOT EXISTS idx_sale_item_spare ON sale_transaction_items (spare_id);

