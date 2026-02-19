-- ============================================================
-- MotorX - Migración V1: Creación inicial de todas las tablas
-- Proyecto: Spark Tech S.A.S
-- Flyway: src/main/resources/db/migration/V1__create_initial_schema.sql
-- Motor: MySQL 8+
-- ============================================================

-- ============================================================
-- TABLA: users
-- Proceso 1: Gestión de usuarios
-- Soporta roles CLIENT, EMPLOYEE, ADMIN
-- ============================================================
CREATE TABLE users (
                       id             BIGINT          NOT NULL AUTO_INCREMENT,
                       name           VARCHAR(150)    NOT NULL,
                       dni            VARCHAR(30)     NOT NULL,
                       email          VARCHAR(150)    NOT NULL,
                       password       VARCHAR(255)    NOT NULL,
                       role           VARCHAR(20)     NOT NULL,
                       enabled        BOOLEAN         NOT NULL DEFAULT FALSE,
                       account_locked BOOLEAN         NOT NULL DEFAULT FALSE,
                       created_at     DATETIME        NOT NULL,
                       updated_at     DATETIME        NOT NULL,

                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT uq_users_dni   UNIQUE (dni),
                       CONSTRAINT chk_users_role CHECK (role IN ('CLIENT', 'EMPLOYEE', 'ADMIN'))
);

CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_dni   ON users (dni);


-- ============================================================
-- TABLA: password_reset_tokens
-- Proceso 1: Recuperación de contraseña
-- Métrica: Tiempo promedio de recuperación
-- ============================================================
CREATE TABLE password_reset_tokens (
                                       id          BIGINT      NOT NULL AUTO_INCREMENT,
                                       token_hash  VARCHAR(255) NOT NULL,
                                       expires_at  DATETIME    NOT NULL,
                                       used        BOOLEAN     NOT NULL DEFAULT FALSE,
                                       created_at  DATETIME    NOT NULL,
                                       used_at     DATETIME    NULL,
                                       user_id     BIGINT      NOT NULL,

                                       CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
                                       CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id)
                                           ON DELETE CASCADE
                                           ON UPDATE CASCADE
);

CREATE INDEX idx_prt_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX idx_prt_user_id    ON password_reset_tokens (user_id);


-- ============================================================
-- TABLA: employees
-- Proceso 1 y 2: Trabajadores del taller
-- Relacionado 1:1 con users
-- ============================================================
CREATE TABLE employees (
                           id         BIGINT       NOT NULL AUTO_INCREMENT,
                           hire_date  DATETIME     NOT NULL,
                           position   VARCHAR(100) NOT NULL,
                           state      VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
                           created_at DATETIME     NOT NULL,
                           updated_at DATETIME     NOT NULL,
                           user_id    BIGINT       NOT NULL,

                           CONSTRAINT pk_employees PRIMARY KEY (id),
                           CONSTRAINT uq_employees_user UNIQUE (user_id),
                           CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id)
                               ON DELETE CASCADE
                               ON UPDATE CASCADE,
                           CONSTRAINT chk_employees_state CHECK (state IN ('AVAILABLE', 'NOT_AVAILABLE'))
);

CREATE INDEX idx_employee_user ON employees (user_id);


-- ============================================================
-- TABLA: vehicles
-- Proceso 2: Vehículos asociados a clientes
-- La cita se agenda sobre un vehículo
-- ============================================================
CREATE TABLE vehicles (
                          id                BIGINT      NOT NULL AUTO_INCREMENT,
                          brand             VARCHAR(100) NOT NULL,
                          model             VARCHAR(100) NOT NULL,
                          license_plate     VARCHAR(10)  NOT NULL,
                          cylinder_capacity INT          NOT NULL,
                          created_at        DATETIME     NOT NULL,
                          updated_at        DATETIME     NOT NULL,
                          user_id           BIGINT       NOT NULL,

                          CONSTRAINT pk_vehicles PRIMARY KEY (id),
                          CONSTRAINT uq_vehicles_license_plate UNIQUE (license_plate),
                          CONSTRAINT fk_vehicles_user FOREIGN KEY (user_id) REFERENCES users (id)
                              ON DELETE RESTRICT
                              ON UPDATE CASCADE
);

CREATE INDEX idx_vehicle_license_plate ON vehicles (license_plate);
CREATE INDEX idx_vehicle_user          ON vehicles (user_id);


-- ============================================================
-- TABLA: services
-- Catálogo de servicios ofrecidos por el taller
-- Proceso 2: El cliente elige un servicio al agendar cita
-- ============================================================
CREATE TABLE services (
                          id                          BIGINT          NOT NULL AUTO_INCREMENT,
                          name                        VARCHAR(150)    NOT NULL,
                          description                 VARCHAR(1000)   NULL,
                          estimated_duration_minutes  INT             NOT NULL,
                          base_price                  DECIMAL(12, 2)  NOT NULL,
                          active                      BOOLEAN         NOT NULL DEFAULT TRUE,
                          created_at                  DATETIME        NOT NULL,
                          updated_at                  DATETIME        NOT NULL,

                          CONSTRAINT pk_services PRIMARY KEY (id),
                          CONSTRAINT uq_services_name UNIQUE (name),
                          CONSTRAINT chk_services_duration CHECK (estimated_duration_minutes > 0),
                          CONSTRAINT chk_services_price    CHECK (base_price >= 0)
);

CREATE INDEX idx_service_name ON services (name);


-- ============================================================
-- TABLA: appointments
-- Proceso 2: Gestión de citas
-- Métricas: ocupación, cancelación, conflictos, historial
-- ============================================================
CREATE TABLE appointments (
                              id                  BIGINT       NOT NULL AUTO_INCREMENT,
                              appointment_date    DATE         NOT NULL,
                              start_time          TIME         NOT NULL,
                              end_time            TIME         NOT NULL,
                              status              VARCHAR(30)  NOT NULL DEFAULT 'SCHEDULED',
                              description         VARCHAR(500) NULL,
                              created_at          DATETIME     NOT NULL,
                              updated_at          DATETIME     NULL,
                              process_started_at  DATETIME     NULL,
                              vehicle_id          BIGINT       NOT NULL,
                              service_id          BIGINT       NOT NULL,

                              CONSTRAINT pk_appointments PRIMARY KEY (id),
                              CONSTRAINT fk_appointments_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
                                  ON DELETE RESTRICT
                                  ON UPDATE CASCADE,
                              CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services (id)
                                  ON DELETE RESTRICT
                                  ON UPDATE CASCADE,
                              CONSTRAINT chk_appointments_status CHECK (
                                  status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED', 'NO_SHOW')
                                  ),
                              CONSTRAINT chk_appointments_times CHECK (end_time > start_time)
);

CREATE INDEX idx_appointment_vehicle ON appointments (vehicle_id);
CREATE INDEX idx_appointment_service ON appointments (service_id);
CREATE INDEX idx_appointment_date    ON appointments (appointment_date);
CREATE INDEX idx_appointment_state   ON appointments (status);

-- Índice compuesto para la consulta de conflictos de horario (la más crítica del proceso 2)
CREATE INDEX idx_appointment_conflict_check ON appointments (appointment_date, status, start_time, end_time);


-- ============================================================
-- TABLA: service_orders
-- Proceso 4: Órdenes de servicio (sprint futuro)
-- Se crea aquí para no romper FK en migraciones posteriores
-- ============================================================
CREATE TABLE service_orders (
                                id              BIGINT          NOT NULL AUTO_INCREMENT,
                                start_date      DATETIME        NOT NULL,
                                end_date        DATETIME        NULL,
                                total_services  DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                                total_spare_parts DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                                total_to_pay    DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                                status          VARCHAR(30)     NOT NULL DEFAULT 'OPEN',
                                created_at      DATETIME        NOT NULL,
                                updated_at      DATETIME        NULL,
                                appointment_id  BIGINT          NOT NULL,
                                employee_id     BIGINT          NOT NULL,

                                CONSTRAINT pk_service_orders PRIMARY KEY (id),
                                CONSTRAINT uq_service_orders_appointment UNIQUE (appointment_id),
                                CONSTRAINT fk_service_orders_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id)
                                    ON DELETE RESTRICT
                                    ON UPDATE CASCADE,
                                CONSTRAINT fk_service_orders_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
                                    ON DELETE RESTRICT
                                    ON UPDATE CASCADE,
                                CONSTRAINT chk_service_orders_status CHECK (
                                    status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'PAID', 'CANCELLED')
                                    ),
                                CONSTRAINT chk_service_orders_totals CHECK (
                                    total_services >= 0 AND total_spare_parts >= 0 AND total_to_pay >= 0
                                    )
);

CREATE INDEX idx_order_appointment ON service_orders (appointment_id);
CREATE INDEX idx_order_employee    ON service_orders (employee_id);
CREATE INDEX idx_order_status      ON service_orders (status);


-- ============================================================
-- TABLA: system_events
-- Logs del sistema para todas las métricas de ambos procesos
-- Proceso 1: registros fallidos, recuperación de contraseña
-- Proceso 2: conflictos de horario, cancelaciones
-- ============================================================
CREATE TABLE system_events (
                               id          BIGINT       NOT NULL AUTO_INCREMENT,
                               event_type  VARCHAR(100) NOT NULL,
                               event_date  DATETIME     NOT NULL,
                               metadata    VARCHAR(500) NULL,
                               severity    VARCHAR(20)  NOT NULL,
                               user_id     BIGINT       NULL,

                               CONSTRAINT pk_system_events PRIMARY KEY (id),
                               CONSTRAINT fk_system_events_user FOREIGN KEY (user_id) REFERENCES users (id)
                                   ON DELETE SET NULL
                                   ON UPDATE CASCADE,
                               CONSTRAINT chk_system_events_type CHECK (
                                   event_type IN (
                                                  'USER_REGISTER_ATTEMPT',
                                                  'USER_REGISTER_SUCCESS',
                                                  'USER_REGISTER_FAILED',
                                                  'PASSWORD_RESET_REQUEST',
                                                  'PASSWORD_RESET_SUCCESS',
                                                  'APPOINTMENT_CREATED',
                                                  'APPOINTMENT_CONFLICT',
                                                  'APPOINTMENT_CANCELLED',
                                                  'LOGIN_SUCCESS',
                                                  'LOGIN_FAILED'
                                       )
                                   ),
                               CONSTRAINT chk_system_events_severity CHECK (
                                   severity IN ('INFO', 'WARNING', 'ERROR')
                                   )
);

CREATE INDEX idx_event_type ON system_events (event_type);
CREATE INDEX idx_event_date ON system_events (event_date);

-- Índice compuesto para consultas de métricas por tipo y rango de fecha
CREATE INDEX idx_event_type_date ON system_events (event_type, event_date);-- ============================================================
-- MotorX - Migración V1: Creación inicial de todas las tablas
-- Proyecto: Spark Tech S.A.S
-- Flyway: src/main/resources/db/migration/V1__create_initial_schema.sql
-- Motor: MySQL 8+
-- ============================================================

-- ============================================================
-- TABLA: users
-- Proceso 1: Gestión de usuarios
-- Soporta roles CLIENT, EMPLOYEE, ADMIN
-- ============================================================
CREATE TABLE users (
                       id             BIGINT          NOT NULL AUTO_INCREMENT,
                       name           VARCHAR(150)    NOT NULL,
                       dni            VARCHAR(30)     NOT NULL,
                       email          VARCHAR(150)    NOT NULL,
                       password       VARCHAR(255)    NOT NULL,
                       role           VARCHAR(20)     NOT NULL,
                       enabled        BOOLEAN         NOT NULL DEFAULT FALSE,
                       account_locked BOOLEAN         NOT NULL DEFAULT FALSE,
                       created_at     DATETIME        NOT NULL,
                       updated_at     DATETIME        NOT NULL,

                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT uq_users_dni   UNIQUE (dni),
                       CONSTRAINT chk_users_role CHECK (role IN ('CLIENT', 'EMPLOYEE', 'ADMIN'))
);

CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_dni   ON users (dni);


-- ============================================================
-- TABLA: password_reset_tokens
-- Proceso 1: Recuperación de contraseña
-- Métrica: Tiempo promedio de recuperación
-- ============================================================
CREATE TABLE password_reset_tokens (
                                       id          BIGINT      NOT NULL AUTO_INCREMENT,
                                       token_hash  VARCHAR(255) NOT NULL,
                                       expires_at  DATETIME    NOT NULL,
                                       used        BOOLEAN     NOT NULL DEFAULT FALSE,
                                       created_at  DATETIME    NOT NULL,
                                       used_at     DATETIME    NULL,
                                       user_id     BIGINT      NOT NULL,

                                       CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
                                       CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id)
                                           ON DELETE CASCADE
                                           ON UPDATE CASCADE
);

CREATE INDEX idx_prt_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX idx_prt_user_id    ON password_reset_tokens (user_id);


-- ============================================================
-- TABLA: employees
-- Proceso 1 y 2: Trabajadores del taller
-- Relacionado 1:1 con users
-- ============================================================
CREATE TABLE employees (
                           id         BIGINT       NOT NULL AUTO_INCREMENT,
                           hire_date  DATETIME     NOT NULL,
                           position   VARCHAR(100) NOT NULL,
                           state      VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
                           created_at DATETIME     NOT NULL,
                           updated_at DATETIME     NOT NULL,
                           user_id    BIGINT       NOT NULL,

                           CONSTRAINT pk_employees PRIMARY KEY (id),
                           CONSTRAINT uq_employees_user UNIQUE (user_id),
                           CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id)
                               ON DELETE CASCADE
                               ON UPDATE CASCADE,
                           CONSTRAINT chk_employees_state CHECK (state IN ('AVAILABLE', 'NOT_AVAILABLE'))
);

CREATE INDEX idx_employee_user ON employees (user_id);


-- ============================================================
-- TABLA: vehicles
-- Proceso 2: Vehículos asociados a clientes
-- La cita se agenda sobre un vehículo
-- ============================================================
CREATE TABLE vehicles (
                          id                BIGINT      NOT NULL AUTO_INCREMENT,
                          brand             VARCHAR(100) NOT NULL,
                          model             VARCHAR(100) NOT NULL,
                          license_plate     VARCHAR(10)  NOT NULL,
                          cylinder_capacity INT          NOT NULL,
                          created_at        DATETIME     NOT NULL,
                          updated_at        DATETIME     NOT NULL,
                          user_id           BIGINT       NOT NULL,

                          CONSTRAINT pk_vehicles PRIMARY KEY (id),
                          CONSTRAINT uq_vehicles_license_plate UNIQUE (license_plate),
                          CONSTRAINT fk_vehicles_user FOREIGN KEY (user_id) REFERENCES users (id)
                              ON DELETE RESTRICT
                              ON UPDATE CASCADE
);

CREATE INDEX idx_vehicle_license_plate ON vehicles (license_plate);
CREATE INDEX idx_vehicle_user          ON vehicles (user_id);


-- ============================================================
-- TABLA: services
-- Catálogo de servicios ofrecidos por el taller
-- Proceso 2: El cliente elige un servicio al agendar cita
-- ============================================================
CREATE TABLE services (
                          id                          BIGINT          NOT NULL AUTO_INCREMENT,
                          name                        VARCHAR(150)    NOT NULL,
                          description                 VARCHAR(1000)   NULL,
                          estimated_duration_minutes  INT             NOT NULL,
                          base_price                  DECIMAL(12, 2)  NOT NULL,
                          active                      BOOLEAN         NOT NULL DEFAULT TRUE,
                          created_at                  DATETIME        NOT NULL,
                          updated_at                  DATETIME        NOT NULL,

                          CONSTRAINT pk_services PRIMARY KEY (id),
                          CONSTRAINT uq_services_name UNIQUE (name),
                          CONSTRAINT chk_services_duration CHECK (estimated_duration_minutes > 0),
                          CONSTRAINT chk_services_price    CHECK (base_price >= 0)
);

CREATE INDEX idx_service_name ON services (name);


-- ============================================================
-- TABLA: appointments
-- Proceso 2: Gestión de citas
-- Métricas: ocupación, cancelación, conflictos, historial
-- ============================================================
CREATE TABLE appointments (
                              id                  BIGINT       NOT NULL AUTO_INCREMENT,
                              appointment_date    DATE         NOT NULL,
                              start_time          TIME         NOT NULL,
                              end_time            TIME         NOT NULL,
                              status              VARCHAR(30)  NOT NULL DEFAULT 'SCHEDULED',
                              description         VARCHAR(500) NULL,
                              created_at          DATETIME     NOT NULL,
                              updated_at          DATETIME     NULL,
                              process_started_at  DATETIME     NULL,
                              vehicle_id          BIGINT       NOT NULL,
                              service_id          BIGINT       NOT NULL,

                              CONSTRAINT pk_appointments PRIMARY KEY (id),
                              CONSTRAINT fk_appointments_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
                                  ON DELETE RESTRICT
                                  ON UPDATE CASCADE,
                              CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services (id)
                                  ON DELETE RESTRICT
                                  ON UPDATE CASCADE,
                              CONSTRAINT chk_appointments_status CHECK (
                                  status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED', 'NO_SHOW')
                                  ),
                              CONSTRAINT chk_appointments_times CHECK (end_time > start_time)
);

CREATE INDEX idx_appointment_vehicle ON appointments (vehicle_id);
CREATE INDEX idx_appointment_service ON appointments (service_id);
CREATE INDEX idx_appointment_date    ON appointments (appointment_date);
CREATE INDEX idx_appointment_state   ON appointments (status);

-- Índice compuesto para la consulta de conflictos de horario (la más crítica del proceso 2)
CREATE INDEX idx_appointment_conflict_check ON appointments (appointment_date, status, start_time, end_time);


-- ============================================================
-- TABLA: service_orders
-- Proceso 4: Órdenes de servicio (sprint futuro)
-- Se crea aquí para no romper FK en migraciones posteriores
-- ============================================================
CREATE TABLE service_orders (
                                id              BIGINT          NOT NULL AUTO_INCREMENT,
                                start_date      DATETIME        NOT NULL,
                                end_date        DATETIME        NULL,
                                total_services  DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                                total_spare_parts DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                                total_to_pay    DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                                status          VARCHAR(30)     NOT NULL DEFAULT 'OPEN',
                                created_at      DATETIME        NOT NULL,
                                updated_at      DATETIME        NULL,
                                appointment_id  BIGINT          NOT NULL,
                                employee_id     BIGINT          NOT NULL,

                                CONSTRAINT pk_service_orders PRIMARY KEY (id),
                                CONSTRAINT uq_service_orders_appointment UNIQUE (appointment_id),
                                CONSTRAINT fk_service_orders_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id)
                                    ON DELETE RESTRICT
                                    ON UPDATE CASCADE,
                                CONSTRAINT fk_service_orders_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
                                    ON DELETE RESTRICT
                                    ON UPDATE CASCADE,
                                CONSTRAINT chk_service_orders_status CHECK (
                                    status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'PAID', 'CANCELLED')
                                    ),
                                CONSTRAINT chk_service_orders_totals CHECK (
                                    total_services >= 0 AND total_spare_parts >= 0 AND total_to_pay >= 0
                                    )
);

CREATE INDEX idx_order_appointment ON service_orders (appointment_id);
CREATE INDEX idx_order_employee    ON service_orders (employee_id);
CREATE INDEX idx_order_status      ON service_orders (status);


-- ============================================================
-- TABLA: system_events
-- Logs del sistema para todas las métricas de ambos procesos
-- Proceso 1: registros fallidos, recuperación de contraseña
-- Proceso 2: conflictos de horario, cancelaciones
-- ============================================================
CREATE TABLE system_events (
                               id          BIGINT       NOT NULL AUTO_INCREMENT,
                               event_type  VARCHAR(100) NOT NULL,
                               event_date  DATETIME     NOT NULL,
                               metadata    VARCHAR(500) NULL,
                               severity    VARCHAR(20)  NOT NULL,
                               user_id     BIGINT       NULL,

                               CONSTRAINT pk_system_events PRIMARY KEY (id),
                               CONSTRAINT fk_system_events_user FOREIGN KEY (user_id) REFERENCES users (id)
                                   ON DELETE SET NULL
                                   ON UPDATE CASCADE,
                               CONSTRAINT chk_system_events_type CHECK (
                                   event_type IN (
                                                  'USER_REGISTER_ATTEMPT',
                                                  'USER_REGISTER_SUCCESS',
                                                  'USER_REGISTER_FAILED',
                                                  'PASSWORD_RESET_REQUEST',
                                                  'PASSWORD_RESET_SUCCESS',
                                                  'APPOINTMENT_CREATED',
                                                  'APPOINTMENT_CONFLICT',
                                                  'APPOINTMENT_CANCELLED',
                                                  'LOGIN_SUCCESS',
                                                  'LOGIN_FAILED'
                                       )
                                   ),
                               CONSTRAINT chk_system_events_severity CHECK (
                                   severity IN ('INFO', 'WARNING', 'ERROR')
                                   )
);

CREATE INDEX idx_event_type ON system_events (event_type);
CREATE INDEX idx_event_date ON system_events (event_date);

-- Índice compuesto para consultas de métricas por tipo y rango de fecha
CREATE INDEX idx_event_type_date ON system_events (event_type, event_date);