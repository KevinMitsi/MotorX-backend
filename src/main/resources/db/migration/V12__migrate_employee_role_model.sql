-- ============================================================
-- MotorX - Migracion V12: Roles operativos por seguridad
-- ============================================================

-- 1) Permitir temporalmente roles legacy y nuevos durante la migracion de datos.
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('CLIENT', 'EMPLOYEE', 'WAREHOUSE_WORKER', 'TECHNICIAN', 'RECEPTIONIST', 'ADMIN'));

-- 2) Migrar usuarios legacy EMPLOYEE a roles operativos segun su posicion.
UPDATE users u
SET role = CASE e.position
    WHEN 'WAREHOUSE_WORKER' THEN 'WAREHOUSE_WORKER'
    WHEN 'RECEPCIONISTA' THEN 'RECEPTIONIST'
    WHEN 'MECANICO' THEN 'TECHNICIAN'
    ELSE 'TECHNICIAN'
END
FROM employees e
WHERE e.user_id = u.id
  AND u.role = 'EMPLOYEE';

-- 3) Fallback defensivo para cuentas EMPLOYEE sin perfil asociado.
UPDATE users
SET role = 'TECHNICIAN'
WHERE role = 'EMPLOYEE';

-- 4) Ajustar check constraint final de roles en users.
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('CLIENT', 'WAREHOUSE_WORKER', 'TECHNICIAN', 'RECEPTIONIST', 'ADMIN'));

