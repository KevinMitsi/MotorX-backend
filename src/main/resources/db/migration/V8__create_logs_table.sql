CREATE TABLE logs (
    id            BIGSERIAL    NOT NULL,
    service_name  VARCHAR(50)  NOT NULL,
    action_type   VARCHAR(60)  NOT NULL,
    result        VARCHAR(20)  NOT NULL,
    actor_email   VARCHAR(150) NULL,
    actor_user_id BIGINT       NULL,
    message       VARCHAR(500) NOT NULL,
    created_at    TIMESTAMP    NOT NULL,

    CONSTRAINT pk_logs PRIMARY KEY (id),
    CONSTRAINT chk_logs_service_name CHECK (
        service_name IN ('AUTHENTICATION','USER','PASSWORD_RESET','APPOINTMENT','VEHICLE','ADMIN')
    ),
    CONSTRAINT chk_logs_action_type CHECK (
        action_type IN (
            'LOGIN','REGISTER','LOGOUT','VERIFY_2FA','REFRESH_TOKEN',
            'PASSWORD_RESET_REQUEST','PASSWORD_RESET_CONFIRM',
            'UPDATE_USER_PROFILE','SCHEDULE_APPOINTMENT','CANCEL_APPOINTMENT'
        )
    ),
    CONSTRAINT chk_logs_result CHECK (result IN ('SUCCESS','FAILURE'))
);

CREATE INDEX idx_logs_created_at     ON logs (created_at);
CREATE INDEX idx_logs_service_action ON logs (service_name, action_type);
CREATE INDEX idx_logs_actor_email    ON logs (actor_email);

