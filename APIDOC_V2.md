# APIDOC V2 - MotorX (Addendum de Cambios)

> **Base funcional:** Esta version incluye **todo lo documentado en `APIDOC.md`**.
> 
> **Adicional V2:** Este documento describe exclusivamente lo nuevo agregado en los 2 commits de la rama actual.
> 
> **Fecha de consolidacion:** 2026-03-31

---

## Commits revisados

1. `d7294f4` - "Agregada log entity y funcionalidad para verificar todo lo que pasa en la aplicacion"
2. `5ec45bc` - "Se agregan las metricas y los tests de esta nueva funcionalidad"

---

## 1) Nueva capacidad: auditoria interna de acciones (logs)

Se agrego un sistema de auditoria persistente para registrar acciones clave de autenticacion, usuarios, recuperacion de contrasena y citas.

### Componentes agregados

- Entidad JPA: `LogEntity` (`logs`)
- Repositorio: `JpaLogRepository`
- Servicio: `ILogService` / `LogServiceImpl`
- Enums:
  - `LogServiceName`: `AUTHENTICATION`, `USER`, `PASSWORD_RESET`, `APPOINTMENT`, `VEHICLE`, `ADMIN`
  - `LogActionType`: `LOGIN`, `REGISTER`, `LOGOUT`, `VERIFY_2FA`, `REFRESH_TOKEN`, `PASSWORD_RESET_REQUEST`, `PASSWORD_RESET_CONFIRM`, `UPDATE_USER_PROFILE`, `SCHEDULE_APPOINTMENT`, `CANCEL_APPOINTMENT`
  - `LogResult`: `SUCCESS`, `FAILURE`
- Migracion Flyway: `V8__create_logs_table.sql`

### Estructura de tabla `logs`

Campos principales:

- `id`
- `service_name`
- `action_type`
- `result`
- `actor_email`
- `actor_user_id`
- `message` (max 500)
- `created_at`

Indices agregados:

- `idx_logs_created_at`
- `idx_logs_service_action`
- `idx_logs_actor_email`

### Flujo funcional cubierto por logs

Se instrumentaron logs de exito/fallo en:

- `AuthServiceImpl`
  - login (incluye bloqueos y credenciales invalidas)
  - registro
  - verificacion 2FA
  - refresh token
  - logout
- `UserServiceImpl`
  - registro de usuario
  - actualizacion de perfil
  - agendar cita
  - cancelar cita propia
- `PasswordResetServiceImpl`
  - solicitud de recuperacion
  - confirmacion de cambio de contrasena

> Nota: En los commits originalmente documentados este modulo era interno y sin endpoint publico.
> En la actualizacion posterior se agrego un endpoint administrativo para consulta de logs (ver seccion 7).

---

## 2) Nueva capacidad: modulo de metricas administrativas

Se agrego el controlador `AdminMetricsController` con acceso restringido a ADMIN.

### Base path

`/api/v1/admin/metrics`

### Seguridad

- Requiere autenticacion JWT
- Requiere rol `ROLE_ADMIN`
- Mantiene el esquema general ya definido en `APIDOC.md` para `/api/v1/admin/**`

### Nuevos endpoints

| Metodo | Endpoint | Descripcion | Respuesta |
|---|---|---|---|
| `GET` | `/api/v1/admin/metrics/performance` | Mide tiempos de respuesta y cumplimiento de umbral por endpoint monitoreado | `List<PerformanceMetricsDTO>` |
| `GET` | `/api/v1/admin/metrics/security` | Resume intentos 401/403 y cumplimiento de control de acceso | `SecurityMetricsDTO` |
| `GET` | `/api/v1/admin/metrics/maintainability` | Metrica estructural (controladores, servicios, repos, estandar de errores, gate JaCoCo) | `MaintainabilityMetricsDTO` |
| `GET` | `/api/v1/admin/metrics/appointments` | Metrica de intentos/exitos/rechazos de creacion de citas y consistencia de datos | `AppointmentsMetricsDTO` |
| `GET` | `/api/v1/admin/metrics/summary` | Consolidado completo de metricas | `MetricsSummaryDTO` |

### DTOs nuevos

#### `PerformanceMetricsDTO`

```json
{
  "endpoint": "/api/auth/login",
  "avgResponseTimeMs": 120,
  "totalRequests": 35,
  "requestsUnderThreshold": 35,
  "compliancePercent": 100.0
}
```

#### `SecurityMetricsDTO`

```json
{
  "unauthorizedAttempts401": 12,
  "forbiddenAttempts403": 3,
  "totalProtectedEndpoints": 40,
  "endpointsWithAuthEnforced": 40,
  "accessControlCompliancePercent": 100.0
}
```

#### `MaintainabilityMetricsDTO`

```json
{
  "totalControllers": 12,
  "totalServices": 15,
  "totalRepositories": 10,
  "standardizedErrorHandlingEnabled": true,
  "jacocoCoverageGatePercent": 60
}
```

#### `AppointmentsMetricsDTO`

```json
{
  "totalCreationAttempts": 80,
  "successfulAppointments": 70,
  "rejectedByBusinessRules": 10,
  "businessRuleCompliancePercent": 100.0,
  "totalAppointmentsInDB": 600,
  "validRecordsInDB": 590,
  "dataIntegrityPercent": 98.33
}
```

#### `MetricsSummaryDTO`

```json
{
  "performance": [
    {
      "endpoint": "/api/auth/login",
      "avgResponseTimeMs": 120,
      "totalRequests": 35,
      "requestsUnderThreshold": 35,
      "compliancePercent": 100.0
    }
  ],
  "security": {
    "unauthorizedAttempts401": 12,
    "forbiddenAttempts403": 3,
    "totalProtectedEndpoints": 40,
    "endpointsWithAuthEnforced": 40,
    "accessControlCompliancePercent": 100.0
  },
  "maintainability": {
    "totalControllers": 12,
    "totalServices": 15,
    "totalRepositories": 10,
    "standardizedErrorHandlingEnabled": true,
    "jacocoCoverageGatePercent": 60
  },
  "appointments": {
    "totalCreationAttempts": 80,
    "successfulAppointments": 70,
    "rejectedByBusinessRules": 10,
    "businessRuleCompliancePercent": 100.0,
    "totalAppointmentsInDB": 600,
    "validRecordsInDB": 590,
    "dataIntegrityPercent": 98.33
  }
}
```

---

## 3) Instrumentacion tecnica de metricas

Ademas de los endpoints admin, se agrego instrumentacion transversal:

- `PerformanceMetricsFilter`
  - Mide tiempo de respuesta para:
    - `/api/auth/login`
    - `/api/auth/verify-2fa`
    - `/api/v1/user/appointments/available-slots`
- `MetricsAuthenticationEntryPoint`
  - Registra intentos `401` cuando se accede a rutas protegidas sin Bearer token valido.
- `MetricsAccessDeniedHandler`
  - Registra intentos `403` de acceso denegado a rutas `/api/v1/admin/**`.

---

## 4) Cambios en la logica de citas para metricas

En `AppointmentServiceImpl` se agregaron contadores para crear metricas de calidad operacional:

- `recordAppointmentCreationAttempt()` al iniciar creacion de cita.
- `recordAppointmentCreationSuccess()` al guardar correctamente.
- `recordAppointmentCreationRejected()` en rechazos por reglas de negocio/excepciones funcionales.

Aplica para:

- `createAppointment(...)` (flujo cliente)
- `createUnplannedAppointment(...)` (flujo admin)

En `JpaAppointmentRepository` se agrego:

- `countValidRecords()` para medir integridad de datos de citas en BD.

---

## 5) Resumen de impacto en API

- Se mantiene vigente todo lo de `APIDOC.md`.
- Se agregan 5 endpoints ADMIN en `/api/v1/admin/metrics/**`.
- Se agrega 1 endpoint ADMIN en `/api/v1/admin/logs` para consulta de auditoria paginada.
- Se agregan capacidades internas de auditoria y observabilidad (logs + metricas).
- Se mantienen respuestas JSON estandar para errores `401` y `403`, ahora instrumentadas para metricas.

---

## 6) Endpoints nuevos para incluir en el resumen global

| Metodo | Endpoint | Acceso |
|---|---|---|
| `GET` | `/api/v1/admin/metrics/performance` | 🔒 Solo ADMIN |
| `GET` | `/api/v1/admin/metrics/security` | 🔒 Solo ADMIN |
| `GET` | `/api/v1/admin/metrics/maintainability` | 🔒 Solo ADMIN |
| `GET` | `/api/v1/admin/metrics/appointments` | 🔒 Solo ADMIN |
| `GET` | `/api/v1/admin/metrics/summary` | 🔒 Solo ADMIN |
| `GET` | `/api/v1/admin/logs` | 🔒 Solo ADMIN |

---

## 7) Actualizacion posterior: consulta administrativa de logs

Se agrego `LogController` para exponer los logs de auditoria con filtros y paginacion sobre la infraestructura ya existente (`LogEntity`, `ILogService`, `LogServiceImpl`, `JpaLogRepository`).

### Base path

`/api/v1/admin/logs`

### Seguridad

- Requiere autenticacion JWT
- Requiere rol `ROLE_ADMIN`
- Respetan politicas de `/api/v1/admin/**`

### Endpoint

| Metodo | Endpoint | Descripcion | Respuesta |
|---|---|---|---|
| `GET` | `/api/v1/admin/logs` | Consulta paginada de logs con filtros opcionales por modulo, accion, resultado, actor y fechas | `LogPageResponseDTO` |

### Query params soportados

- `serviceName` (`AUTHENTICATION`, `USER`, `PASSWORD_RESET`, `APPOINTMENT`, `VEHICLE`, `ADMIN`)
- `actionType` (`LOGIN`, `REGISTER`, `LOGOUT`, `VERIFY_2FA`, `REFRESH_TOKEN`, `PASSWORD_RESET_REQUEST`, `PASSWORD_RESET_CONFIRM`, `UPDATE_USER_PROFILE`, `SCHEDULE_APPOINTMENT`, `CANCEL_APPOINTMENT`)
- `result` (`SUCCESS`, `FAILURE`)
- `actorEmail` (coincidencia parcial)
- `actorUserId`
- `from` (ISO-8601 `LocalDateTime`)
- `to` (ISO-8601 `LocalDateTime`)
- Paginacion Spring: `page`, `size`, `sort`

Valores por defecto de paginacion:

- `size=20`
- `sort=createdAt,desc`

### Ejemplo de respuesta (`LogPageResponseDTO`)

```json
{
  "content": [
    {
      "id": 7,
      "serviceName": "AUTHENTICATION",
      "actionType": "LOGIN",
      "result": "SUCCESS",
      "actorEmail": "admin@motorx.com",
      "actorUserId": 5,
      "message": "Inicio de sesion exitoso",
      "createdAt": "2026-03-31T10:15:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "empty": false
}
```

### Respuestas de error documentadas

- `400` parametros invalidos (por ejemplo enums no validos)
- `401` no autenticado
- `403` sin permisos ADMIN
- `500` error interno del servidor

---

> Este `APIDOC_V2.md` funciona como complemento de `APIDOC.md`: no reemplaza la documentacion original, la extiende con los cambios introducidos en los commits `d7294f4` y `5ec45bc`.

