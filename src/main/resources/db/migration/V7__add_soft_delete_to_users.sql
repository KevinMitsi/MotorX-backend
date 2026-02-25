-- ============================================================
-- MotorX - Migración V7: Soft Delete para la tabla users
-- Proyecto: Spark Tech S.A.S
-- Motivo: Permite eliminar lógicamente un usuario sin borrar
--         su historial de citas ni sus vehículos asociados.
--         Un usuario con deleted_at != NULL se considera eliminado.
-- ============================================================

-- 1. Agregar columna deleted_at (NULL = activo, NOT NULL = eliminado)
ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL;

-- 2. Índice para acelerar consultas de usuarios activos (WHERE deleted_at IS NULL)
CREATE INDEX idx_user_deleted_at ON users (deleted_at);

