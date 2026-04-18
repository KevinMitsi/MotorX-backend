-- ============================================================
-- MotorX - Migracion V12: Roles operativos por seguridad
-- ============================================================

-- 1) Migrar usuarios legacy EMPLOYEE a roles operativos segun su posicion.
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

-- 2) Fallback defensivo para cuentas EMPLOYEE sin perfil asociado.
UPDATE users
SET role = 'TECHNICIAN'
WHERE role = 'EMPLOYEE';

-- 3) Ajustar check constraint de roles en users.
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('CLIENT', 'WAREHOUSE_WORKER', 'TECHNICIAN', 'RECEPTIONIST', 'ADMIN'));

