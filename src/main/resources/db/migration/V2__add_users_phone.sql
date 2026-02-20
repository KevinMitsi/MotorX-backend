ALTER TABLE users
ADD COLUMN phone VARCHAR(20);

-- Insertar usuario admin con contraseña bcrypt hardcodeada (si no existe)
INSERT INTO users (name, dni, email, password, role, enabled, account_locked, created_at, updated_at, phone)
SELECT
    'Administrator',
    '00000000',
    'admin@admin.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'ADMIN',
    TRUE,
    FALSE,
    NOW(),
    NOW(),
    NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@admin.com');
