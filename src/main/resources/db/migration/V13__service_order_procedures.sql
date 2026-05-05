-- ============================================================
-- MotorX - Migracion V13: Procedimientos y ordenes de servicio
-- ============================================================

-- 1) Catalogo de procedimientos
CREATE TABLE procedures (
    id BIGSERIAL NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_procedures PRIMARY KEY (id),
    CONSTRAINT uq_procedures_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_procedures_name ON procedures (name);

-- 2) Relacion servicios - procedimientos base
CREATE TABLE service_procedures (
    service_id BIGINT NOT NULL,
    procedure_id BIGINT NOT NULL,

    CONSTRAINT pk_service_procedures PRIMARY KEY (service_id, procedure_id),
    CONSTRAINT fk_service_procedures_service FOREIGN KEY (service_id) REFERENCES services (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_service_procedures_procedure FOREIGN KEY (procedure_id) REFERENCES procedures (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_service_procedures_service ON service_procedures (service_id);
CREATE INDEX IF NOT EXISTS idx_service_procedures_procedure ON service_procedures (procedure_id);

-- 3) Procedimientos usados en una orden de servicio
CREATE TABLE order_procedures (
    order_id BIGINT NOT NULL,
    procedure_id BIGINT NOT NULL,
    cost DECIMAL(12,2) NOT NULL,

    CONSTRAINT pk_order_procedures PRIMARY KEY (order_id, procedure_id),
    CONSTRAINT fk_order_procedures_order FOREIGN KEY (order_id) REFERENCES service_orders (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_procedures_procedure FOREIGN KEY (procedure_id) REFERENCES procedures (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_order_procedures_cost CHECK (cost >= 0)
);

CREATE INDEX IF NOT EXISTS idx_order_procedures_order ON order_procedures (order_id);
CREATE INDEX IF NOT EXISTS idx_order_procedures_procedure ON order_procedures (procedure_id);

-- 4) Repuestos usados en una orden de servicio
CREATE TABLE order_spares (
    order_id BIGINT NOT NULL,
    spare_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,

    CONSTRAINT pk_order_spares PRIMARY KEY (order_id, spare_id),
    CONSTRAINT fk_order_spares_order FOREIGN KEY (order_id) REFERENCES service_orders (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_spares_spare FOREIGN KEY (spare_id) REFERENCES spares (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_order_spares_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_spares_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_order_spares_order ON order_spares (order_id);
CREATE INDEX IF NOT EXISTS idx_order_spares_spare ON order_spares (spare_id);

