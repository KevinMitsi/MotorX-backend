-- ============================================================
-- MotorX - Migracion V11: Umbral de stock en repuestos
-- ============================================================

ALTER TABLE spares
    ADD COLUMN IF NOT EXISTS stock_threshold INT NOT NULL DEFAULT 0;

ALTER TABLE spares
    DROP CONSTRAINT IF EXISTS chk_spares_stock_threshold;

ALTER TABLE spares
    ADD CONSTRAINT chk_spares_stock_threshold CHECK (stock_threshold >= 0);

