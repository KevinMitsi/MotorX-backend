-- ============================================================
-- MotorX - Migración V1: Creación inicial de todas las tablas
-- Proyecto: Spark Tech S.A.S
-- Flyway: src/main/resources/db/migration/V1__create_initial_schema.sql
-- Motor: PostgreSQL 14+
-- ============================================================

-- TABLA: users
CREATE TABLE users (
                       id             BIGSERIAL       NOT NULL,
                       name           VARCHAR(150)    NOT NULL,
                       dni            VARCHAR(30)     NOT NULL,
                       email          VARCHAR(150)    NOT NULL,
                       password       VARCHAR(255)    NOT NULL,
                       role           VARCHAR(20)     NOT NULL,
                       enabled        BOOLEAN         NOT NULL DEFAULT FALSE,
                       account_locked BOOLEAN         NOT NULL DEFAULT FALSE,
                       created_at     TIMESTAMP       NOT NULL,
                       updated_at     TIMESTAMP       NOT NULL,

                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT uq_users_dni   UNIQUE (dni),
                       CONSTRAINT chk_users_role CHECK (role IN ('CLIENT', 'EMPLOYEE', 'ADMIN'))
);

CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_dni   ON users (dni);


-- TABLA: password_reset_tokens
CREATE TABLE password_reset_tokens (
                                       id          BIGSERIAL       NOT NULL,
                                       token_hash  VARCHAR(255)    NOT NULL,
                                       expires_at  TIMESTAMP       NOT NULL,
                                       used        BOOLEAN         NOT NULL DEFAULT FALSE,
                                       created_at  TIMESTAMP       NOT NULL,
                                       used_at     TIMESTAMP       NULL,
                                       user_id     BIGINT          NOT NULL,

                                       CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
                                       CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id)
                                           ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_prt_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX idx_prt_user_id    ON password_reset_tokens (user_id);


-- TABLA: employees
CREATE TABLE employees (
                           id         BIGSERIAL       NOT NULL,
                           hire_date  TIMESTAMP       NOT NULL,
                           position   VARCHAR(100)    NOT NULL,
                           state      VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
                           created_at TIMESTAMP       NOT NULL,
                           updated_at TIMESTAMP       NOT NULL,
                           user_id    BIGINT          NOT NULL,

                           CONSTRAINT pk_employees PRIMARY KEY (id),
                           CONSTRAINT uq_employees_user UNIQUE (user_id),
                           CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id)
                               ON DELETE CASCADE ON UPDATE CASCADE,
                           CONSTRAINT chk_employees_state CHECK (state IN ('AVAILABLE', 'NOT_AVAILABLE'))
);

CREATE INDEX idx_employee_user ON employees (user_id);


-- TABLA: vehicles
CREATE TABLE vehicles (
                          id                BIGSERIAL       NOT NULL,
                          brand             VARCHAR(100)    NOT NULL,
                          model             VARCHAR(100)    NOT NULL,
                          license_plate     VARCHAR(10)     NOT NULL,
                          cylinder_capacity INT             NOT NULL,
                          created_at        TIMESTAMP       NOT NULL,
                          updated_at        TIMESTAMP       NOT NULL,
                          user_id           BIGINT          NOT NULL,

                          CONSTRAINT pk_vehicles PRIMARY KEY (id),
                          CONSTRAINT uq_vehicles_license_plate UNIQUE (license_plate),
                          CONSTRAINT fk_vehicles_user FOREIGN KEY (user_id) REFERENCES users (id)
                              ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX idx_vehicle_license_plate ON vehicles (license_plate);
CREATE INDEX idx_vehicle_user          ON vehicles (user_id);


-- TABLA: services
CREATE TABLE services (
                          id                         BIGSERIAL       NOT NULL,
                          name                       VARCHAR(150)    NOT NULL,
                          description                VARCHAR(1000)   NULL,
                          estimated_duration_minutes INT             NOT NULL,
                          base_price                 DECIMAL(12, 2)  NOT NULL,
                          active                     BOOLEAN         NOT NULL DEFAULT TRUE,
                          created_at                 TIMESTAMP       NOT NULL,
                          updated_at                 TIMESTAMP       NOT NULL,

                          CONSTRAINT pk_services PRIMARY KEY (id),
                          CONSTRAINT uq_services_name UNIQUE (name),
                          CONSTRAINT chk_services_duration CHECK (estimated_duration_minutes > 0),
                          CONSTRAINT chk_services_price    CHECK (base_price >= 0)
);

CREATE INDEX idx_service_name ON services (name);


-- TABLA: appointments
CREATE TABLE appointments (
                              id                  BIGSERIAL       NOT NULL,
                              appointment_date    DATE            NOT NULL,
                              start_time          TIME            NOT NULL,
                              end_time            TIME            NOT NULL,
                              status              VARCHAR(30)     NOT NULL DEFAULT 'SCHEDULED',
                              description         VARCHAR(500)    NULL,
                              created_at          TIMESTAMP       NOT NULL,
                              updated_at          TIMESTAMP       NULL,
                              process_started_at  TIMESTAMP       NULL,
                              vehicle_id          BIGINT          NOT NULL,
                              service_id          BIGINT          NOT NULL,

                              CONSTRAINT pk_appointments PRIMARY KEY (id),
                              CONSTRAINT fk_appointments_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
                                  ON DELETE RESTRICT ON UPDATE CASCADE,
                              CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services (id)
                                  ON DELETE RESTRICT ON UPDATE CASCADE,
                              CONSTRAINT chk_appointments_status CHECK (
                                  status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED','REJECTED','NO_SHOW')
                                  ),
                              CONSTRAINT chk_appointments_times CHECK (end_time > start_time)
);

CREATE INDEX idx_appointment_vehicle       ON appointments (vehicle_id);
CREATE INDEX idx_appointment_service       ON appointments (service_id);
CREATE INDEX idx_appointment_date          ON appointments (appointment_date);
CREATE INDEX idx_appointment_state         ON appointments (status);
-- Índice compuesto crítico para existsConflict() del JpaAppointmentRepository
CREATE INDEX idx_appointment_conflict_check ON appointments (appointment_date, status, start_time, end_time);


-- TABLA: service_orders
CREATE TABLE service_orders (
                                id                BIGSERIAL       NOT NULL,
                                start_date        TIMESTAMP       NOT NULL,
                                end_date          TIMESTAMP       NULL,
                                total_services    DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                                total_spare_parts DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                                total_to_pay      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                                status            VARCHAR(30)     NOT NULL DEFAULT 'OPEN',
                                created_at        TIMESTAMP       NOT NULL,
                                updated_at        TIMESTAMP       NULL,
                                appointment_id    BIGINT          NOT NULL,
                                employee_id       BIGINT          NOT NULL,

                                CONSTRAINT pk_service_orders PRIMARY KEY (id),
                                CONSTRAINT uq_service_orders_appointment UNIQUE (appointment_id),
                                CONSTRAINT fk_service_orders_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id)
                                    ON DELETE RESTRICT ON UPDATE CASCADE,
                                CONSTRAINT fk_service_orders_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
                                    ON DELETE RESTRICT ON UPDATE CASCADE,
                                CONSTRAINT chk_service_orders_status CHECK (
                                    status IN ('OPEN','IN_PROGRESS','COMPLETED','PAID','CANCELLED')
                                    ),
                                CONSTRAINT chk_service_orders_totals CHECK (
                                    total_services >= 0 AND total_spare_parts >= 0 AND total_to_pay >= 0
                                    )
);

CREATE INDEX idx_order_appointment ON service_orders (appointment_id);
CREATE INDEX idx_order_employee    ON service_orders (employee_id);
CREATE INDEX idx_order_status      ON service_orders (status);


-- TABLA: system_events
CREATE TABLE system_events (
                               id          BIGSERIAL       NOT NULL,
                               event_type  VARCHAR(100)    NOT NULL,
                               event_date  TIMESTAMP       NOT NULL,
                               metadata    VARCHAR(500)    NULL,
                               severity    VARCHAR(20)     NOT NULL,
                               user_id     BIGINT          NULL,

                               CONSTRAINT pk_system_events PRIMARY KEY (id),
                               CONSTRAINT fk_system_events_user FOREIGN KEY (user_id) REFERENCES users (id)
                                   ON DELETE SET NULL ON UPDATE CASCADE,
                               CONSTRAINT chk_system_events_type CHECK (
                                   event_type IN (
                                                  'USER_REGISTER_ATTEMPT','USER_REGISTER_SUCCESS','USER_REGISTER_FAILED',
                                                  'PASSWORD_RESET_REQUEST','PASSWORD_RESET_SUCCESS',
                                                  'APPOINTMENT_CREATED','APPOINTMENT_CONFLICT','APPOINTMENT_CANCELLED',
                                                  'LOGIN_SUCCESS','LOGIN_FAILED'
                                       )
                                   ),
                               CONSTRAINT chk_system_events_severity CHECK (
                                   severity IN ('INFO','WARNING','ERROR')
                                   )
);

CREATE INDEX idx_event_type      ON system_events (event_type);
CREATE INDEX idx_event_date      ON system_events (event_date);
CREATE INDEX idx_event_type_date ON system_events (event_type, event_date);