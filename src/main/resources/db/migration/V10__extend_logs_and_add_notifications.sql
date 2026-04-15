-- ============================================================
-- MotorX - Migracion V10: Logs para inventario/recepcion/repuestos y modulo de notificaciones
-- ============================================================

ALTER TABLE logs DROP CONSTRAINT IF EXISTS chk_logs_service_name;
ALTER TABLE logs ADD CONSTRAINT chk_logs_service_name CHECK (
    service_name IN (
        'AUTHENTICATION','USER','PASSWORD_RESET','APPOINTMENT','VEHICLE','ADMIN',
        'SPARE','INVENTORY','RECEPTION','NOTIFICATION'
    )
);

ALTER TABLE logs DROP CONSTRAINT IF EXISTS chk_logs_action_type;
ALTER TABLE logs ADD CONSTRAINT chk_logs_action_type CHECK (
    action_type IN (
        'LOGIN','REGISTER','LOGOUT','VERIFY_2FA','REFRESH_TOKEN',
        'PASSWORD_RESET_REQUEST','PASSWORD_RESET_CONFIRM',
        'UPDATE_USER_PROFILE','SCHEDULE_APPOINTMENT','CANCEL_APPOINTMENT',
        'CREATE_SPARE','UPDATE_SPARE','UPDATE_SPARE_PURCHASE_PRICE','DELETE_SPARE',
        'REGISTER_PURCHASE','REGISTER_SALE',
        'INITIATE_RECEPTION','CONFIRM_RECEPTION',
        'CREATE_NOTIFICATION','READ_NOTIFICATION','READ_ALL_NOTIFICATIONS'
    )
);

CREATE TABLE notifications (
    id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    source VARCHAR(80) NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_notifications_urgency CHECK (urgency IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_notifications_user_created_at ON notifications (user_id, created_at);
CREATE INDEX idx_notifications_user_is_read ON notifications (user_id, is_read);

