# 📘 MotorX API — Documentación de Endpoints

> **Base URL:** `/api`  
> **Autenticación:** Bearer Token (JWT) en el header `Authorization: Bearer <token>`  
> **Formato:** Todas las peticiones y respuestas son `application/json`  
> **Fecha de generación:** 2026-02-28

---

## 📑 Tabla de Contenidos

1. [Información General](#información-general)
2. [Enums del Sistema](#enums-del-sistema)
3. [DTO de Error Global](#dto-de-error-global)
4. [Autenticación — `/api/auth`](#1-autenticación--apiauth)
5. [Recuperación de Contraseña — `/api/password-reset`](#2-recuperación-de-contraseña--apipassword-reset)
6. [User — Citas — `/api/v1/user/appointments`](#3-user--citas--apiv1userappointments)
7. [User — Vehículos — `/api/v1/user/vehicles`](#4-user--vehículos--apiv1uservehicles)
8. [Admin — Citas — `/api/v1/admin/appointments`](#5-admin--citas--apiv1adminappointments)
9. [Admin — Empleados — `/api/v1/admin/employees`](#6-admin--empleados--apiv1adminemployees)
10. [Admin — Usuarios — `/api/v1/admin/users`](#7-admin--usuarios--apiv1adminusers)
11. [Admin — Vehículos — `/api/v1/admin/vehicles`](#8-admin--vehículos--apiv1adminvehicles)

---

## Información General

### Reglas de Seguridad (Security Filter Chain)

| Patrón de ruta | Acceso |
|---|---|
| `/api/auth/**` | 🌐 **Público** — No requiere autenticación |
| `/api/password-reset/**` | 🌐 **Público** — No requiere autenticación |
| `/api/public/**` | 🌐 **Público** — No requiere autenticación |
| `/swagger-ui/**`, `/v3/api-docs/**` | 🌐 **Público** — Documentación Swagger |
| `/actuator/health` | 🌐 **Público** — Health check |
| `/api/v1/admin/**` | 🔒 **Solo ADMIN** — Requiere rol `ROLE_ADMIN` |
| `/api/v1/user/**` | 🔑 **Autenticado** — Cualquier usuario con sesión activa |
| Cualquier otra ruta | 🔑 **Autenticado** |

### Roles del Sistema

| Rol | Descripción |
|---|---|
| `CLIENT` | Usuario final (cliente del taller) |
| `EMPLOYEE` | Empleado del taller (mecánico, recepcionista) |
| `ADMIN` | Administrador con acceso total |

---

## Enums del Sistema

### `AppointmentType` — Tipos de Cita

| Valor | Descripción | Restricción de Marca |
|---|---|---|
| `MANUAL_WARRANTY_REVIEW` | Revisión de garantía de manual. Mañana: 7:00 AM · Tarde: 1:00 PM | Solo **Auteco** |
| `AUTECO_WARRANTY` | Garantía Auteco (motos en periodo de garantía). Mañana: 7:30 AM · Tarde: 1:15 PM | Solo **Auteco** |
| `QUICK_SERVICE` | Servicio rápido. Mañana: 7:15 AM · Tarde: 1:30 PM | Cualquier marca |
| `MAINTENANCE` | Mantenimiento general. Mañana: 7:45 AM (sin recepción por la tarde) | Cualquier marca |
| `OIL_CHANGE` | Cambio de aceite (slots cada 30 min). Mañana: 8:00–10:00 · Tarde: 2:00–4:30 | Cualquier marca |
| `UNPLANNED` | Cita no planeada — Solo el administrador puede crearla | Cualquier marca |
| `REWORK` | Reproceso — No se puede agendar en línea, requiere contacto directo | N/A |

### `AppointmentStatus` — Estados de Cita

| Valor | Descripción |
|---|---|
| `SCHEDULED` | Cita agendada y pendiente |
| `IN_PROGRESS` | Cita en progreso |
| `COMPLETED` | Cita completada |
| `CANCELLED` | Cita cancelada |
| `REJECTED` | Cita rechazada |
| `NO_SHOW` | El cliente no se presentó |

### `EmployeePosition` — Cargos de Empleado

| Valor | Descripción |
|---|---|
| `RECEPCIONISTA` | Recepcionista del taller |
| `MECANICO` | Mecánico del taller |

### `EmployeeState` — Estado del Empleado

| Valor | Descripción |
|---|---|
| `AVAILABLE` | Disponible para asignación |
| `NOT_AVAILABLE` | No disponible |

---

## DTO de Error Global

Todas las respuestas de error siguen este formato estándar:

### `ResponseErrorDTO`

```json
{
  "code": 400,
  "message": "Descripción del tipo de error",
  "details": {
    "detalle": "Mensaje específico del error",
    "campo": "Información adicional si aplica"
  }
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `code` | `Integer` | Código HTTP del error (400, 401, 403, 404, 409, etc.) |
| `message` | `String` | Mensaje descriptivo del tipo de error |
| `details` | `Object` | Objeto con detalles adicionales. En errores de validación es un `Map<String, String>` con campo→mensaje |

---

## 1. Autenticación — `/api/auth`

> 🌐 **Acceso público** (excepto `/me` y `/logout` que requieren autenticación)  
> **Servicio:** `IAuthService` — Gestiona el registro, login con 2FA, generación de tokens JWT y sesión del usuario.

---

### 1.1 `POST /api/auth/login`

**Descripción:** Inicia sesión con email y contraseña. Si el usuario tiene rol `ADMIN`, retorna el token JWT directamente. Para los demás roles (`CLIENT`, `EMPLOYEE`), genera un código de verificación 2FA que se envía al email del usuario; en ese caso la respuesta indica que el código fue enviado.

**Acceso:** 🌐 Público

#### Request Body — `LoginRequestDTO`

```json
{
  "email": "usuario@example.com",
  "password": "miContraseña123"
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `email` | `String` | `@NotBlank`, `@Email` | ✅ |
| `password` | `String` | `@NotBlank` | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | **ADMIN:** Login exitoso, retorna token | `AuthResponseDTO` |
| `200` | **Otros roles:** Código 2FA enviado al email | `Object` (mensaje de confirmación) |
| `400` | Datos de entrada inválidos | `ResponseErrorDTO` |
| `401` | Credenciales inválidas / Cuenta bloqueada | `ResponseErrorDTO` |

#### Response (ADMIN) — `AuthResponseDTO`

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "type": "Bearer",
  "userId": 1,
  "email": "admin@motorx.com",
  "name": "Admin Principal",
  "role": "ADMIN"
}
```

---

### 1.2 `POST /api/auth/verify-2fa`

**Descripción:** Verifica el código de 6 dígitos enviado al email del usuario durante el login. Si el código es correcto y no ha expirado, retorna el token JWT.

**Acceso:** 🌐 Público

#### Request Body — `Verify2FADTO`

```json
{
  "email": "usuario@example.com",
  "code": "123456"
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `email` | `String` | `@NotBlank`, `@Email` | ✅ |
| `code` | `String` | `@NotBlank`, `@Pattern(^\d{6}$)` — Exactamente 6 dígitos | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Verificación exitosa | `AuthResponseDTO` |
| `400` | Código inválido o expirado | `ResponseErrorDTO` |
| `401` | Código incorrecto | `ResponseErrorDTO` |

---

### 1.3 `POST /api/auth/register`

**Descripción:** Registra un nuevo usuario con rol `CLIENT`. El email y el DNI deben ser únicos en el sistema. Al registrarse exitosamente, retorna el token JWT para iniciar sesión inmediatamente.

**Acceso:** 🌐 Público

#### Request Body — `RegisterUserDTO`

```json
{
  "name": "Juan Pérez",
  "dni": "1234567890",
  "email": "juan@example.com",
  "password": "MiPass123!",
  "phone": "+57 310 1234567"
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `name` | `String` | `@NotBlank`, `@Size(max=150)` | ✅ |
| `dni` | `String` | `@NotBlank`, `@Size(max=30)` | ✅ |
| `email` | `String` | `@NotBlank`, `@Email`, `@Size(max=150)` | ✅ |
| `password` | `String` | `@NotBlank`, `@Size(min=6, max=100)` | ✅ |
| `phone` | `String` | `@NotBlank`, `@Pattern(^[0-9+()\\-\\s]{7,20}$)`, `@Size(max=20)` | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Usuario registrado exitosamente | `AuthResponseDTO` |
| `400` | Datos inválidos o email/DNI ya registrados | `ResponseErrorDTO` |

---

### 1.4 `GET /api/auth/me`

**Descripción:** Retorna la información completa del usuario actualmente autenticado.

**Acceso:** 🔑 Autenticado (`@PreAuthorize("isAuthenticated()")`)

#### Headers requeridos

```
Authorization: Bearer <token>
```

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Datos del usuario retornados | `UserDTO` |
| `401` | No autenticado | `ResponseErrorDTO` |
| `404` | Usuario no encontrado | `ResponseErrorDTO` |

#### Response — `UserDTO`

```json
{
  "id": 1,
  "name": "Juan Pérez",
  "dni": "1234567890",
  "email": "juan@example.com",
  "password": null,
  "phone": "+57 310 1234567",
  "createdAt": "2026-01-15T10:30:00",
  "role": "CLIENT",
  "enabled": true,
  "accountLocked": false,
  "updatedAt": "2026-02-01T14:00:00"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | ID único del usuario |
| `name` | `String` | Nombre completo |
| `dni` | `String` | Documento de identidad |
| `email` | `String` | Correo electrónico |
| `password` | `String` | Siempre `null` por seguridad en práctica |
| `phone` | `String` | Teléfono de contacto |
| `createdAt` | `LocalDateTime` | Fecha de creación de la cuenta |
| `role` | `Role` | Rol del usuario: `CLIENT`, `EMPLOYEE`, `ADMIN` |
| `enabled` | `boolean` | Si la cuenta está habilitada |
| `accountLocked` | `boolean` | Si la cuenta está bloqueada |
| `updatedAt` | `LocalDateTime` | Última actualización |

---

### 1.5 `GET /api/auth/logout`

**Descripción:** Cierra la sesión del usuario invalidando su token JWT actual.

**Acceso:** 🔑 Autenticado (`@PreAuthorize("isAuthenticated()")`)

#### Headers requeridos

```
Authorization: Bearer <token>
```

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Logout exitoso | `"Logout exitoso"` (String) |
| `401` | No autenticado | `ResponseErrorDTO` |

---

### 1.6 `POST /api/auth/refresh`

**Descripción:** Genera un nuevo access token a partir del refresh token. Usar cuando el token actual está próximo a expirar.

**Acceso:** 🌐 Público

#### Query Parameters

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `refreshToken` | `String` | ✅ | El refresh token obtenido durante el login |

#### Ejemplo

```
POST /api/auth/refresh?refreshToken=eyJhbGciOiJIUzI1NiIs...
```

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Token renovado exitosamente | `AuthResponseDTO` |
| `401` | Refresh token inválido o expirado | `ResponseErrorDTO` |

---

### DTO de Respuesta — `AuthResponseDTO`

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "type": "Bearer",
  "userId": 1,
  "email": "usuario@example.com",
  "name": "Juan Pérez",
  "role": "CLIENT"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `token` | `String` | Token JWT para autenticación |
| `type` | `String` | Siempre `"Bearer"` |
| `userId` | `Long` | ID del usuario autenticado |
| `email` | `String` | Email del usuario |
| `name` | `String` | Nombre completo del usuario |
| `role` | `Role` | Rol: `CLIENT`, `EMPLOYEE` o `ADMIN` |

---

## 2. Recuperación de Contraseña — `/api/password-reset`

> 🌐 **Acceso público** — No requiere autenticación  
> **Servicio:** `IPasswordResetService` — Genera un token de recuperación, lo envía por email y valida el reseteo de la contraseña.

---

### 2.1 `POST /api/password-reset/request`

**Descripción:** Envía un código/token de recuperación al email indicado. Por seguridad, la respuesta siempre es `200` independientemente de si el email existe en el sistema (para no revelar qué emails están registrados).

**Acceso:** 🌐 Público

#### Request Body — `PasswordResetRequestDTO`

```json
{
  "email": "usuario@example.com"
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `email` | `String` | `@NotBlank`, `@Email` | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Solicitud procesada (código enviado si el email existe) | `"If the email exists, a recovery code has been sent."` (String) |
| `400` | Datos de entrada inválidos | `ResponseErrorDTO` |

---

### 2.2 `PUT /api/password-reset`

**Descripción:** Valida el token de recuperación recibido por email y establece la nueva contraseña del usuario.

**Acceso:** 🌐 Público

#### Request Body — `PasswordResetDTO`

```json
{
  "token": "abc123-recovery-token",
  "newPassword": "NuevaPass123!"
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `token` | `String` | `@NotBlank` | ✅ |
| `newPassword` | `String` | `@NotBlank`, `@Length(min=8, max=20)`, debe contener al menos 1 mayúscula, 1 número y 1 símbolo (`@$!%*?&._-`) | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Contraseña restablecida exitosamente | `"Password has been successfully reset."` (String) |
| `400` | Token inválido, expirado o datos incorrectos | `ResponseErrorDTO` |

---

## 3. User — Citas — `/api/v1/user/appointments`

> 🔑 **Acceso:** Cualquier usuario autenticado  
> **Servicio:** `IUserService` — Permite a los clientes consultar disponibilidad, verificar pico y placa, agendar citas (con asignación automática de técnico), consultar su historial y cancelar sus propias citas.

---

### 3.1 `GET /api/v1/user/appointments/available-slots`

**Descripción:** Devuelve los horarios disponibles para agendar una cita de un tipo específico en una fecha determinada. Un slot se muestra como disponible si al menos un técnico tiene ese horario libre. Se recomienda llamar **después** de verificar pico y placa.

**Acceso:** 🔑 Autenticado

#### Query Parameters

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `date` | `LocalDate` | ✅ | Fecha deseada en formato `yyyy-MM-dd` |
| `type` | `AppointmentType` | ✅ | Tipo de cita (ver enum arriba) |

#### Ejemplo

```
GET /api/v1/user/appointments/available-slots?date=2026-03-15&type=OIL_CHANGE
```

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Slots consultados exitosamente | `AvailableSlotsResponseDTO` |
| `400` | Fecha o tipo de cita inválidos | `ResponseErrorDTO` |
| `401` | No autenticado | `ResponseErrorDTO` |

#### Response — `AvailableSlotsResponseDTO`

```json
{
  "date": "2026-03-15",
  "appointmentType": "OIL_CHANGE",
  "availableSlots": [
    {
      "startTime": "08:00",
      "endTime": "08:30",
      "availableTechnicians": 3
    },
    {
      "startTime": "08:30",
      "endTime": "09:00",
      "availableTechnicians": 2
    }
  ]
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `date` | `LocalDate` | Fecha consultada |
| `appointmentType` | `AppointmentType` | Tipo de cita consultado |
| `availableSlots` | `List<AvailableSlotDTO>` | Lista de horarios disponibles |
| `availableSlots[].startTime` | `LocalTime` | Hora de inicio del slot |
| `availableSlots[].endTime` | `LocalTime` | Hora de fin del slot |
| `availableSlots[].availableTechnicians` | `int` | Cantidad de técnicos disponibles en ese slot |

---

### 3.2 `GET /api/v1/user/appointments/check-plate-restriction`

**Descripción:** Verifica si el vehículo tiene restricción de movilidad (pico y placa) en la fecha indicada. Se recomienda llamar **antes** de mostrar los slots de horarios disponibles. Si hay restricción, retorna `409` con los datos de contacto para casos urgentes.

**Acceso:** 🔑 Autenticado

#### Query Parameters

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `vehicleId` | `Long` | ✅ | ID del vehículo a verificar |
| `date` | `LocalDate` | ✅ | Fecha a verificar en formato `yyyy-MM-dd` |

#### Ejemplo

```
GET /api/v1/user/appointments/check-plate-restriction?vehicleId=5&date=2026-03-15
```

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `409` | Respuesta siempre con código 409 (ver campo `message` para determinar si hay restricción) | `LicensePlateRestrictionResponseDTO` |
| `404` | Vehículo no encontrado | `ResponseErrorDTO` |

#### Response — `LicensePlateRestrictionResponseDTO`

**Con restricción:**
```json
{
  "vehiclePlate": "ABC123",
  "restrictedDate": "2026-03-15",
  "message": "La moto con placa ABC123 tiene restricción de movilidad (pico y placa) el 2026-03-15. No es posible agendar la cita para ese día.",
  "urgentContactMessage": "Si tu cita es urgente, puedes llamarnos dentro del horario de atención. Ten en cuenta que no podemos garantizar la disponibilidad.",
  "phoneNumber": "+57 310 8402499",
  "businessHours": "Lunes a Viernes 7:00 AM - 5:30 PM (excepto 12:00 - 1:00 PM)"
}
```

**Sin restricción:**
```json
{
  "vehiclePlate": "ABC123",
  "restrictedDate": "2026-03-15",
  "message": "La moto con placa ABC123 no tiene restricción de movilidad (pico y placa) el 2026-03-15. Puedes proceder a agendar tu cita sin problemas.",
  "urgentContactMessage": null,
  "phoneNumber": null,
  "businessHours": null
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `vehiclePlate` | `String` | Placa del vehículo consultado |
| `restrictedDate` | `LocalDate` | Fecha consultada |
| `message` | `String` | Mensaje descriptivo (indica si hay o no restricción) |
| `urgentContactMessage` | `String?` | Mensaje para contacto urgente (solo si hay restricción) |
| `phoneNumber` | `String?` | Teléfono de contacto (solo si hay restricción) |
| `businessHours` | `String?` | Horario de atención (solo si hay restricción) |

---

### 3.3 `GET /api/v1/user/appointments/rework-info`

**Descripción:** Devuelve los datos de contacto del taller para agendar un reproceso. Los reprocesos **no pueden agendarse en línea** y requieren comunicación directa con el taller por WhatsApp o llamada.

**Acceso:** 🔑 Autenticado

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Información de contacto retornada | `ReworkRedirectResponseDTO` |

#### Response — `ReworkRedirectResponseDTO`

```json
{
  "message": "Los reprocesos requieren atención personalizada. Por favor contáctanos directamente para agendar tu cita.",
  "whatsappLink": "https://wa.me/573108402499",
  "phoneNumber": "+57 310 8402499",
  "businessHours": "Lunes a Viernes 7:00 AM - 5:30 PM (excepto 12:00 - 1:00 PM)"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `message` | `String` | Mensaje informativo para el usuario |
| `whatsappLink` | `String` | Enlace directo a WhatsApp del taller |
| `phoneNumber` | `String` | Número de teléfono del taller |
| `businessHours` | `String` | Horario de atención |

---

### 3.4 `POST /api/v1/user/appointments`

**Descripción:** Agenda una nueva cita para el usuario autenticado. El sistema valida automáticamente: pico y placa, compatibilidad de marca con el tipo de cita, horario dentro de los permitidos y disponibilidad de técnicos. El técnico se asigna automáticamente. El kilometraje se registra como referencia para el historial de mantenimiento.

**Acceso:** 🔑 Autenticado

#### Request Body — `CreateAppointmentRequestDTO`

```json
{
  "vehicleId": 5,
  "appointmentType": "OIL_CHANGE",
  "appointmentDate": "2026-03-20",
  "startTime": "08:00",
  "currentMileage": 15000,
  "clientNotes": ["Ruido extraño en el motor", "Revisar frenos"]
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `vehicleId` | `Long` | `@NotNull` | ✅ |
| `appointmentType` | `AppointmentType` | `@NotNull` — No puede ser `UNPLANNED` ni `REWORK` | ✅ |
| `appointmentDate` | `LocalDate` | `@NotNull`, `@Future` — Debe ser fecha futura | ✅ |
| `startTime` | `LocalTime` | `@NotNull` — Debe coincidir con un slot válido para el tipo | ✅ |
| `currentMileage` | `Integer` | `@NotNull`, `@Min(0)` | ✅ |
| `clientNotes` | `Set<String>` | Sin restricción | ❌ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `201` | Cita agendada exitosamente | `AppointmentResponseDTO` |
| `400` | Datos inválidos, kilometraje negativo o horario no permitido | `ResponseErrorDTO` |
| `404` | Vehículo no encontrado | `ResponseErrorDTO` |
| `409` | Sin técnicos disponibles, pico y placa, o marca no permitida para el tipo | `ResponseErrorDTO` |

---

### 3.5 `GET /api/v1/user/appointments/my`

**Descripción:** Lista el historial completo de citas del cliente autenticado, incluyendo todas las citas en cualquier estado (agendadas, completadas, canceladas, etc.).

**Acceso:** 🔑 Autenticado

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Historial de citas retornado | `List<AppointmentResponseDTO>` |

---

### 3.6 `GET /api/v1/user/appointments/my/{appointmentId}`

**Descripción:** Devuelve el detalle de una cita específica del cliente autenticado. Si la cita no pertenece al usuario, retorna error.

**Acceso:** 🔑 Autenticado

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `appointmentId` | `Long` | ID de la cita |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Detalle de la cita | `AppointmentResponseDTO` |
| `403` | La cita no pertenece al usuario | `ResponseErrorDTO` |
| `404` | Cita no encontrada | `ResponseErrorDTO` |

---

### 3.7 `GET /api/v1/user/appointments/my/vehicle/{vehicleId}`

**Descripción:** Lista el historial de citas de un vehículo específico del cliente autenticado.

**Acceso:** 🔑 Autenticado

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `vehicleId` | `Long` | ID del vehículo |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Historial de citas del vehículo | `List<AppointmentResponseDTO>` |

---

### 3.8 `DELETE /api/v1/user/appointments/my/{appointmentId}`

**Descripción:** Cancela una cita del cliente autenticado. Solo se pueden cancelar citas que pertenezcan al usuario. La cita cambia su estado a `CANCELLED`.

**Acceso:** 🔑 Autenticado

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `appointmentId` | `Long` | ID de la cita a cancelar |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Cita cancelada exitosamente | `AppointmentResponseDTO` |
| `403` | La cita no pertenece al usuario | `ResponseErrorDTO` |
| `404` | Cita no encontrada | `ResponseErrorDTO` |

---

### DTO de Respuesta — `AppointmentResponseDTO`

```json
{
  "id": 1,
  "appointmentType": "OIL_CHANGE",
  "status": "SCHEDULED",
  "appointmentDate": "2026-03-20",
  "startTime": "08:00",
  "endTime": "08:30",
  "vehicleId": 5,
  "vehiclePlate": "ABC12D",
  "vehicleBrand": "Yamaha",
  "vehicleModel": "FZ 250",
  "clientId": 10,
  "clientFullName": "Juan Pérez",
  "clientEmail": "juan@example.com",
  "technicianId": 3,
  "technicianFullName": "Carlos Técnico",
  "currentMileage": 15000,
  "clientNotes": "Ruido extraño en el motor",
  "adminNotes": null,
  "createdAt": "2026-03-01T10:30:00",
  "updatedAt": "2026-03-01T10:30:00"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | ID único de la cita |
| `appointmentType` | `AppointmentType` | Tipo de cita |
| `status` | `AppointmentStatus` | Estado actual de la cita |
| `appointmentDate` | `LocalDate` | Fecha de la cita |
| `startTime` | `LocalTime` | Hora de inicio |
| `endTime` | `LocalTime` | Hora de fin |
| `vehicleId` | `Long` | ID del vehículo |
| `vehiclePlate` | `String` | Placa del vehículo |
| `vehicleBrand` | `String` | Marca del vehículo |
| `vehicleModel` | `String` | Modelo del vehículo |
| `clientId` | `Long` | ID del cliente |
| `clientFullName` | `String` | Nombre completo del cliente |
| `clientEmail` | `String` | Email del cliente |
| `technicianId` | `Long?` | ID del técnico asignado (puede ser `null`) |
| `technicianFullName` | `String?` | Nombre del técnico (puede ser `null`) |
| `currentMileage` | `Integer` | Kilometraje registrado al momento de la cita |
| `clientNotes` | `String?` | Notas del cliente |
| `adminNotes` | `String?` | Notas del administrador |
| `createdAt` | `LocalDateTime` | Fecha de creación |
| `updatedAt` | `LocalDateTime` | Última modificación |

---

## 4. User — Vehículos — `/api/v1/user/vehicles`

> 🔑 **Acceso:** Cualquier usuario autenticado  
> **Servicio:** `IVehicleService` — CRUD completo de vehículos del cliente autenticado. Valida formato de placa colombiana, unicidad de placa y número de chasis, y que el vehículo pertenezca al usuario para operaciones de lectura, actualización y eliminación.

---

### 4.1 `POST /api/v1/user/vehicles`

**Descripción:** Registra una nueva moto en la lista del cliente autenticado. La placa debe tener formato colombiano (`AAA12A`). Si la placa ya pertenece a otro usuario, se indica que contacte al administrador para transferir la propiedad. Si el número de chasis ya existe, también se rechaza.

**Acceso:** 🔑 Autenticado

#### Request Body — `CreateVehicleRequestDTO`

```json
{
  "brand": "Yamaha",
  "model": "FZ 250",
  "yearOfManufacture": 2024,
  "licensePlate": "ABC12D",
  "cylinderCapacity": 250,
  "chassisNumber": "9C6KE091080123456"
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `brand` | `String` | `@NotBlank`, `@Size(max=100)` | ✅ |
| `model` | `String` | `@NotBlank`, `@Size(max=100)` | ✅ |
| `yearOfManufacture` | `Integer` | `@NotNull`, `@Min(1950)`, `@Max(2026)` | ✅ |
| `licensePlate` | `String` | `@NotBlank`, `@Pattern(^[A-Z]{3}\d{2}[A-Z])` — Formato colombiano de motos | ✅ |
| `cylinderCapacity` | `Integer` | `@NotNull`, `@Min(50)`, `@Max(9999)` — En centímetros cúbicos | ✅ |
| `chassisNumber` | `String` | `@NotBlank`, `@Size(max=50)` — Tal como aparece en la tarjeta de propiedad | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `201` | Vehículo registrado exitosamente | `VehicleResponseDTO` |
| `400` | Datos inválidos, placa con formato incorrecto o año fuera de rango | `ResponseErrorDTO` |
| `409` | La placa o el número de chasis ya están registrados | `ResponseErrorDTO` |

---

### 4.2 `GET /api/v1/user/vehicles`

**Descripción:** Devuelve todos los vehículos registrados del cliente autenticado.

**Acceso:** 🔑 Autenticado

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Lista de vehículos retornada | `List<VehicleResponseDTO>` |

---

### 4.3 `GET /api/v1/user/vehicles/{vehicleId}`

**Descripción:** Obtiene el detalle de un vehículo que pertenezca al usuario autenticado.

**Acceso:** 🔑 Autenticado

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `vehicleId` | `Long` | ID del vehículo |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Vehículo encontrado | `VehicleResponseDTO` |
| `403` | El vehículo no pertenece al usuario | `ResponseErrorDTO` |
| `404` | Vehículo no encontrado | `ResponseErrorDTO` |

---

### 4.4 `PUT /api/v1/user/vehicles/{vehicleId}`

**Descripción:** Actualiza la marca, modelo y cilindraje de un vehículo propio. La placa, el número de chasis y el año de fabricación **NO son modificables** ya que son datos del documento oficial del vehículo.

**Acceso:** 🔑 Autenticado

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `vehicleId` | `Long` | ID del vehículo a actualizar |

#### Request Body — `UpdateVehicleRequestDTO`

```json
{
  "brand": "Yamaha",
  "model": "MT-03",
  "cylinderCapacity": 321
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `brand` | `String` | `@NotBlank`, `@Size(max=100)` | ✅ |
| `model` | `String` | `@NotBlank`, `@Size(max=100)` | ✅ |
| `cylinderCapacity` | `Integer` | `@NotNull`, `@Min(50)`, `@Max(9999)` | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Vehículo actualizado | `VehicleResponseDTO` |
| `400` | Datos inválidos | `ResponseErrorDTO` |
| `403` | El vehículo no pertenece al usuario | `ResponseErrorDTO` |
| `404` | Vehículo no encontrado | `ResponseErrorDTO` |

---

### 4.5 `DELETE /api/v1/user/vehicles/{vehicleId}`

**Descripción:** Elimina un vehículo de la lista del cliente autenticado.

**Acceso:** 🔑 Autenticado

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `vehicleId` | `Long` | ID del vehículo a eliminar |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `204` | Vehículo eliminado (sin contenido) | — |
| `403` | El vehículo no pertenece al usuario | `ResponseErrorDTO` |
| `404` | Vehículo no encontrado | `ResponseErrorDTO` |

---

### DTO de Respuesta — `VehicleResponseDTO`

```json
{
  "id": 5,
  "brand": "Yamaha",
  "model": "FZ 250",
  "yearOfManufacture": 2024,
  "licensePlate": "ABC12D",
  "cylinderCapacity": 250,
  "chassisNumber": "9C6KE091080123456",
  "ownerId": 10,
  "ownerName": "Juan Pérez",
  "ownerEmail": "juan@example.com",
  "createdAt": "2026-01-15T10:30:00",
  "updatedAt": "2026-02-01T14:00:00"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | ID único del vehículo |
| `brand` | `String` | Marca de la moto |
| `model` | `String` | Modelo de la moto |
| `yearOfManufacture` | `Integer` | Año de fabricación |
| `licensePlate` | `String` | Placa del vehículo |
| `cylinderCapacity` | `Integer` | Cilindraje en cc |
| `chassisNumber` | `String` | Número de chasis |
| `ownerId` | `Long` | ID del propietario |
| `ownerName` | `String` | Nombre del propietario |
| `ownerEmail` | `String` | Email del propietario |
| `createdAt` | `LocalDateTime` | Fecha de registro |
| `updatedAt` | `LocalDateTime` | Última modificación |

---

## 5. Admin — Citas — `/api/v1/admin/appointments`

> 🔒 **Acceso:** Solo `ROLE_ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`)  
> **Servicio:** `IAdminService` — Proporciona al administrador visibilidad total de la agenda (diaria, por rango de fechas), permite consultar slots disponibles, registrar citas no planeadas, cancelar cualquier cita, cambiar técnicos asignados, y consultar el historial de citas por cliente o vehículo.

---

### 5.1 `GET /api/v1/admin/appointments/agenda`

**Descripción:** Lista todas las citas de una fecha específica, ordenadas por hora. Ideal para que el administrador vea la agenda del día.

**Acceso:** 🔒 Solo ADMIN

#### Query Parameters

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `date` | `LocalDate` | ✅ | Fecha a consultar en formato `yyyy-MM-dd` |

#### Ejemplo

```
GET /api/v1/admin/appointments/agenda?date=2026-03-15
```

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Agenda del día retornada | `List<AppointmentResponseDTO>` |

---

### 5.2 `GET /api/v1/admin/appointments/calendar`

**Descripción:** Lista todas las citas dentro de un rango de fechas para mostrar en una vista de calendario. Incluye citas en todos los estados.

**Acceso:** 🔒 Solo ADMIN

#### Query Parameters

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `start` | `LocalDate` | ✅ | Fecha de inicio del rango (inclusive) `yyyy-MM-dd` |
| `end` | `LocalDate` | ✅ | Fecha de fin del rango (inclusive) `yyyy-MM-dd` |

#### Ejemplo

```
GET /api/v1/admin/appointments/calendar?start=2026-03-01&end=2026-03-31
```

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Citas del rango retornadas | `List<AppointmentResponseDTO>` |

---

### 5.3 `GET /api/v1/admin/appointments/available-slots`

**Descripción:** Consulta los slots disponibles para cualquier fecha y tipo de cita. Funciona igual que el endpoint de usuario pero accesible para el administrador.

**Acceso:** 🔒 Solo ADMIN

#### Query Parameters

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `date` | `LocalDate` | ✅ | Fecha a consultar `yyyy-MM-dd` |
| `type` | `AppointmentType` | ✅ | Tipo de cita |

#### Ejemplo

```
GET /api/v1/admin/appointments/available-slots?date=2026-03-15&type=QUICK_SERVICE
```

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Slots disponibles retornados | `AvailableSlotsResponseDTO` |

---

### 5.4 `POST /api/v1/admin/appointments/unplanned`

**Descripción:** Registra una cita no planeada (tipo `UNPLANNED`). Permite al administrador crear citas fuera de los horarios de recepción estándar, en espacios donde no hubo cita previa. El técnico puede asignarse manualmente (pasando `technicianId`) o automáticamente (dejando `technicianId` en `null`).

**Acceso:** 🔒 Solo ADMIN

#### Request Body — `CreateUnplannedAppointmentRequestDTO`

```json
{
  "vehicleId": 5,
  "appointmentType": "UNPLANNED",
  "appointmentDate": "2026-03-15",
  "startTime": "09:00",
  "currentMileage": 12000,
  "technicianId": 3,
  "adminNotes": "Cliente llegó sin cita previa, se le atiende en espacio libre"
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `vehicleId` | `Long` | `@NotNull` | ✅ |
| `appointmentType` | `AppointmentType` | `@NotNull` | ✅ |
| `appointmentDate` | `LocalDate` | `@NotNull` | ✅ |
| `startTime` | `LocalTime` | `@NotNull` | ✅ |
| `currentMileage` | `Integer` | `@NotNull`, `@Min(0)` | ✅ |
| `technicianId` | `Long` | Sin restricción — Si es `null`, se asigna automáticamente | ❌ |
| `adminNotes` | `String` | Sin restricción | ❌ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `201` | Cita no planeada registrada | `AppointmentResponseDTO` |
| `400` | Datos inválidos | `ResponseErrorDTO` |
| `404` | Vehículo o técnico no encontrado | `ResponseErrorDTO` |
| `409` | Técnico no disponible o restricción de pico y placa | `ResponseErrorDTO` |

---

### 5.5 `PATCH /api/v1/admin/appointments/{appointmentId}/cancel`

**Descripción:** Cancela cualquier cita del sistema. El administrador debe indicar el motivo de cancelación y puede elegir si enviar una notificación por email al cliente afectado.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `appointmentId` | `Long` | ID de la cita a cancelar |

#### Request Body — `CancelAppointmentRequestDTO`

```json
{
  "reason": "Técnico no disponible por emergencia",
  "notifyClient": true
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `reason` | `String` | `@NotNull` — Motivo de cancelación | ✅ |
| `notifyClient` | `boolean` | Sin restricción — Enviar notificación al cliente | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Cita cancelada exitosamente | `AppointmentResponseDTO` |
| `404` | Cita no encontrada | `ResponseErrorDTO` |

---

### 5.6 `PATCH /api/v1/admin/appointments/{appointmentId}/technician`

**Descripción:** Cambia el técnico asignado a una cita **sin modificar el horario**. El sistema valida que el nuevo técnico tenga ese slot libre. El administrador puede elegir si notificar al cliente por email.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `appointmentId` | `Long` | ID de la cita |

#### Request Body — `UpdateAppointmentTechnicianRequestDTO`

```json
{
  "newTechnicianId": 7,
  "notifyClient": true
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `newTechnicianId` | `Long` | `@NotNull` | ✅ |
| `notifyClient` | `boolean` | Sin restricción — Enviar notificación al cliente | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Técnico actualizado exitosamente | `AppointmentResponseDTO` |
| `404` | Cita o técnico no encontrado | `ResponseErrorDTO` |
| `409` | El nuevo técnico tiene ese horario ocupado | `ResponseErrorDTO` |

---

### 5.7 `GET /api/v1/admin/appointments/{appointmentId}`

**Descripción:** Devuelve el detalle completo de cualquier cita del sistema, sin importar el cliente o estado.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `appointmentId` | `Long` | ID de la cita |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Detalle de la cita retornado | `AppointmentResponseDTO` |
| `404` | Cita no encontrada | `ResponseErrorDTO` |

---

### 5.8 `GET /api/v1/admin/appointments/client/{clientId}`

**Descripción:** Lista todo el historial de citas de un cliente específico, incluyendo todos los estados.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `clientId` | `Long` | ID del cliente |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Historial del cliente retornado | `List<AppointmentResponseDTO>` |

---

### 5.9 `GET /api/v1/admin/appointments/vehicle/{vehicleId}`

**Descripción:** Lista todo el historial de citas de un vehículo específico, útil para ver el mantenimiento completo de una moto.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `vehicleId` | `Long` | ID del vehículo |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Historial del vehículo retornado | `List<AppointmentResponseDTO>` |

---

## 6. Admin — Empleados — `/api/v1/admin/employees`

> 🔒 **Acceso:** Solo `ROLE_ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`)  
> **Servicio:** `IEmployeeService` — CRUD completo de empleados. Al crear un empleado, también crea su cuenta de usuario con rol `EMPLOYEE`. Permite actualizar cargo y estado (disponible/no disponible), y eliminar empleados junto con su cuenta de usuario asociada.

---

### 6.1 `POST /api/v1/admin/employees`

**Descripción:** Crea un nuevo empleado y automáticamente le crea una cuenta de usuario en la aplicación con rol `EMPLOYEE`. El empleado queda habilitado y en estado `AVAILABLE` desde el inicio. El email y DNI del usuario deben ser únicos.

**Acceso:** 🔒 Solo ADMIN

#### Request Body — `CreateEmployeeRequestDTO`

```json
{
  "position": "MECANICO",
  "user": {
    "name": "Carlos Mecánico",
    "dni": "9876543210",
    "email": "carlos@motorx.com",
    "password": "TempPass123!",
    "phone": "+57 300 1234567"
  }
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `position` | `EmployeePosition` | `@NotNull` — Valores: `RECEPCIONISTA`, `MECANICO` | ✅ |
| `user` | `RegisterUserDTO` | `@NotNull`, `@Valid` — Se aplican las validaciones de `RegisterUserDTO` | ✅ |
| `user.name` | `String` | `@NotBlank`, `@Size(max=150)` | ✅ |
| `user.dni` | `String` | `@NotBlank`, `@Size(max=30)` | ✅ |
| `user.email` | `String` | `@NotBlank`, `@Email`, `@Size(max=150)` | ✅ |
| `user.password` | `String` | `@NotBlank`, `@Size(min=6, max=100)` | ✅ |
| `user.phone` | `String` | `@NotBlank`, `@Pattern(^[0-9+()\\-\\s]{7,20}$)`, `@Size(max=20)` | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `201` | Empleado creado exitosamente | `EmployeeResponseDTO` |
| `400` | Datos inválidos | `ResponseErrorDTO` |
| `409` | El email o DNI ya están registrados | `ResponseErrorDTO` |

---

### 6.2 `GET /api/v1/admin/employees`

**Descripción:** Devuelve la lista completa de todos los empleados registrados con sus datos de usuario asociados.

**Acceso:** 🔒 Solo ADMIN

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Lista de empleados retornada | `List<EmployeeResponseDTO>` |

---

### 6.3 `GET /api/v1/admin/employees/{employeeId}`

**Descripción:** Obtiene la información completa de un empleado, incluyendo sus datos de usuario.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `employeeId` | `Long` | ID del empleado |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Empleado encontrado | `EmployeeResponseDTO` |
| `404` | Empleado no encontrado | `ResponseErrorDTO` |

---

### 6.4 `PUT /api/v1/admin/employees/{employeeId}`

**Descripción:** Actualiza el cargo y el estado del empleado. Permite cambiar entre `RECEPCIONISTA`/`MECANICO` y entre `AVAILABLE`/`NOT_AVAILABLE`.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `employeeId` | `Long` | ID del empleado a actualizar |

#### Request Body — `UpdateEmployeeRequestDTO`

```json
{
  "position": "MECANICO",
  "state": "NOT_AVAILABLE"
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `position` | `EmployeePosition` | `@NotNull` — Valores: `RECEPCIONISTA`, `MECANICO` | ✅ |
| `state` | `EmployeeState` | `@NotNull` — Valores: `AVAILABLE`, `NOT_AVAILABLE` | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Empleado actualizado | `EmployeeResponseDTO` |
| `400` | Datos inválidos | `ResponseErrorDTO` |
| `404` | Empleado no encontrado | `ResponseErrorDTO` |

---

### 6.5 `DELETE /api/v1/admin/employees/{employeeId}`

**Descripción:** Elimina el empleado y su cuenta de usuario asociada del sistema. **Esta operación es irreversible.**

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `employeeId` | `Long` | ID del empleado a eliminar |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `204` | Empleado eliminado (sin contenido) | — |
| `404` | Empleado no encontrado | `ResponseErrorDTO` |

---

### DTO de Respuesta — `EmployeeResponseDTO`

```json
{
  "employeeId": 3,
  "position": "MECANICO",
  "state": "AVAILABLE",
  "hireDate": "2025-06-15T09:00:00",
  "userId": 15,
  "userName": "Carlos Mecánico",
  "userEmail": "carlos@motorx.com",
  "userDni": "9876543210",
  "userPhone": "+57 300 1234567",
  "createdAt": "2025-06-15T09:00:00",
  "updatedAt": "2026-02-01T14:00:00"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `employeeId` | `Long` | ID único del empleado |
| `position` | `EmployeePosition` | Cargo: `RECEPCIONISTA` o `MECANICO` |
| `state` | `EmployeeState` | Estado: `AVAILABLE` o `NOT_AVAILABLE` |
| `hireDate` | `LocalDateTime` | Fecha de contratación |
| `userId` | `Long` | ID del usuario asociado |
| `userName` | `String` | Nombre completo del usuario |
| `userEmail` | `String` | Email del usuario |
| `userDni` | `String` | DNI del usuario |
| `userPhone` | `String` | Teléfono del usuario |
| `createdAt` | `LocalDateTime` | Fecha de creación del registro |
| `updatedAt` | `LocalDateTime` | Última modificación |

---

## 7. Admin — Usuarios — `/api/v1/admin/users`

> 🔒 **Acceso:** Solo `ROLE_ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`)  
> **Servicio:** `IAdminUserService` — Gestión administrativa de usuarios: listado completo (incluyendo eliminados lógicamente), consulta individual, bloqueo/desbloqueo de cuentas y eliminación lógica (soft delete) que preserva el historial.

---

### 7.1 `GET /api/v1/admin/users`

**Descripción:** Devuelve la lista completa de todos los usuarios registrados en el sistema, **incluyendo los eliminados lógicamente** (aquellos con `deletedAt != null`).

**Acceso:** 🔒 Solo ADMIN

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Lista de usuarios retornada | `List<AdminUserResponseDTO>` |

---

### 7.2 `GET /api/v1/admin/users/{userId}`

**Descripción:** Obtiene la información completa de un usuario por su ID.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `userId` | `Long` | ID del usuario |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Usuario encontrado | `AdminUserResponseDTO` |
| `404` | Usuario no encontrado | `ResponseErrorDTO` |

---

### 7.3 `PATCH /api/v1/admin/users/{userId}/block`

**Descripción:** Bloquea la cuenta de un usuario, impidiendo que pueda iniciar sesión. Si la cuenta ya está bloqueada, retorna `409 Conflict`.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `userId` | `Long` | ID del usuario a bloquear |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Usuario bloqueado exitosamente | `AdminUserResponseDTO` |
| `404` | Usuario no encontrado | `ResponseErrorDTO` |
| `409` | El usuario ya se encuentra bloqueado | `ResponseErrorDTO` |

---

### 7.4 `PATCH /api/v1/admin/users/{userId}/unblock`

**Descripción:** Reactiva la cuenta de un usuario previamente bloqueado, permitiéndole volver a iniciar sesión.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `userId` | `Long` | ID del usuario a desbloquear |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Usuario desbloqueado exitosamente | `AdminUserResponseDTO` |
| `404` | Usuario no encontrado | `ResponseErrorDTO` |

---

### 7.5 `DELETE /api/v1/admin/users/{userId}`

**Descripción:** Realiza una **eliminación lógica (soft delete)** del usuario: establece `deletedAt` con la fecha actual, desactiva la cuenta (`enabled = false`) y la bloquea (`accountLocked = true`). **No borra el registro** de la base de datos para preservar el historial de citas y vehículos asociados. Si el usuario ya fue eliminado, retorna `409 Conflict`.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `userId` | `Long` | ID del usuario a eliminar |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `204` | Usuario eliminado lógicamente (sin contenido) | — |
| `404` | Usuario no encontrado | `ResponseErrorDTO` |
| `409` | El usuario ya ha sido eliminado | `ResponseErrorDTO` |

---

### DTO de Respuesta — `AdminUserResponseDTO`

```json
{
  "id": 10,
  "name": "Juan Pérez",
  "dni": "1234567890",
  "email": "juan@example.com",
  "phone": "+57 310 1234567",
  "role": "CLIENT",
  "enabled": true,
  "accountLocked": false,
  "createdAt": "2026-01-15T10:30:00",
  "updatedAt": "2026-02-01T14:00:00",
  "deletedAt": null
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | ID único del usuario |
| `name` | `String` | Nombre completo |
| `dni` | `String` | Documento de identidad |
| `email` | `String` | Correo electrónico |
| `phone` | `String` | Teléfono de contacto |
| `role` | `Role` | Rol: `CLIENT`, `EMPLOYEE` o `ADMIN` |
| `enabled` | `boolean` | Si la cuenta está habilitada (`false` si fue eliminada) |
| `accountLocked` | `boolean` | Si la cuenta está bloqueada |
| `createdAt` | `LocalDateTime` | Fecha de creación de la cuenta |
| `updatedAt` | `LocalDateTime` | Última modificación |
| `deletedAt` | `LocalDateTime?` | Fecha de eliminación lógica (`null` si no está eliminado) |

---

## 8. Admin — Vehículos — `/api/v1/admin/vehicles`

> 🔒 **Acceso:** Solo `ROLE_ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`)  
> **Servicio:** `IEmployeeService` — Permite al administrador consultar todos los vehículos del sistema y transferir la propiedad de un vehículo entre usuarios.

---

### 8.1 `GET /api/v1/admin/vehicles`

**Descripción:** Devuelve todos los vehículos registrados en el sistema, sin importar el propietario.

**Acceso:** 🔒 Solo ADMIN

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Lista de vehículos retornada | `List<VehicleResponseDTO>` |

---

### 8.2 `GET /api/v1/admin/vehicles/{vehicleId}`

**Descripción:** Obtiene la información completa de cualquier vehículo del sistema.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `vehicleId` | `Long` | ID del vehículo |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Vehículo encontrado | `VehicleResponseDTO` |
| `404` | Vehículo no encontrado | `ResponseErrorDTO` |

---

### 8.3 `PATCH /api/v1/admin/vehicles/{vehicleId}/transfer-ownership`

**Descripción:** Transfiere la propiedad de una moto de un usuario a otro. El vehículo se elimina del dueño original y se asigna al nuevo propietario. El nuevo dueño debe ser un **cliente activo** (no eliminado ni bloqueado). Se valida que el nuevo dueño no tenga ya la misma placa registrada.

**Acceso:** 🔒 Solo ADMIN

#### Path Parameters

| Parámetro | Tipo | Descripción |
|---|---|---|
| `vehicleId` | `Long` | ID del vehículo a transferir |

#### Request Body — `TransferVehicleOwnershipRequestDTO`

```json
{
  "newOwnerId": 15
}
```

| Campo | Tipo | Validación | Obligatorio |
|---|---|---|---|
| `newOwnerId` | `Long` | `@NotNull` — ID del nuevo propietario | ✅ |

#### Respuestas

| Código | Descripción | Body |
|---|---|---|
| `200` | Propiedad transferida exitosamente | `VehicleResponseDTO` |
| `400` | El nuevo dueño no es válido o ya tiene el vehículo | `ResponseErrorDTO` |
| `404` | Vehículo o usuario no encontrado | `ResponseErrorDTO` |
| `409` | Conflicto de propiedad del vehículo | `ResponseErrorDTO` |

---

## 📋 Resumen Rápido de Todos los Endpoints

### 🌐 Públicos (sin autenticación)

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/auth/login` | Iniciar sesión |
| `POST` | `/api/auth/verify-2fa` | Verificar código 2FA |
| `POST` | `/api/auth/register` | Registrar nuevo usuario |
| `POST` | `/api/auth/refresh` | Renovar token JWT |
| `POST` | `/api/password-reset/request` | Solicitar código de recuperación |
| `PUT` | `/api/password-reset` | Confirmar reseteo de contraseña |

### 🔑 Autenticados (cualquier usuario con sesión)

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/auth/me` | Obtener usuario actual |
| `GET` | `/api/auth/logout` | Cerrar sesión |
| `GET` | `/api/v1/user/appointments/available-slots` | Consultar slots disponibles |
| `GET` | `/api/v1/user/appointments/check-plate-restriction` | Verificar pico y placa |
| `GET` | `/api/v1/user/appointments/rework-info` | Info contacto para reprocesos |
| `POST` | `/api/v1/user/appointments` | Agendar una cita |
| `GET` | `/api/v1/user/appointments/my` | Mis citas |
| `GET` | `/api/v1/user/appointments/my/{appointmentId}` | Detalle de mi cita |
| `GET` | `/api/v1/user/appointments/my/vehicle/{vehicleId}` | Citas de mi vehículo |
| `DELETE` | `/api/v1/user/appointments/my/{appointmentId}` | Cancelar mi cita |
| `POST` | `/api/v1/user/vehicles` | Registrar vehículo |
| `GET` | `/api/v1/user/vehicles` | Listar mis vehículos |
| `GET` | `/api/v1/user/vehicles/{vehicleId}` | Detalle de mi vehículo |
| `PUT` | `/api/v1/user/vehicles/{vehicleId}` | Actualizar mi vehículo |
| `DELETE` | `/api/v1/user/vehicles/{vehicleId}` | Eliminar mi vehículo |

### 🔒 Solo ADMIN

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v1/admin/appointments/agenda` | Agenda del día |
| `GET` | `/api/v1/admin/appointments/calendar` | Vista de calendario |
| `GET` | `/api/v1/admin/appointments/available-slots` | Slots disponibles (admin) |
| `POST` | `/api/v1/admin/appointments/unplanned` | Registrar cita no planeada |
| `PATCH` | `/api/v1/admin/appointments/{appointmentId}/cancel` | Cancelar cualquier cita |
| `PATCH` | `/api/v1/admin/appointments/{appointmentId}/technician` | Cambiar técnico |
| `GET` | `/api/v1/admin/appointments/{appointmentId}` | Detalle de cualquier cita |
| `GET` | `/api/v1/admin/appointments/client/{clientId}` | Historial de un cliente |
| `GET` | `/api/v1/admin/appointments/vehicle/{vehicleId}` | Historial de un vehículo |
| `POST` | `/api/v1/admin/employees` | Crear empleado |
| `GET` | `/api/v1/admin/employees` | Listar empleados |
| `GET` | `/api/v1/admin/employees/{employeeId}` | Detalle de empleado |
| `PUT` | `/api/v1/admin/employees/{employeeId}` | Actualizar empleado |
| `DELETE` | `/api/v1/admin/employees/{employeeId}` | Eliminar empleado |
| `GET` | `/api/v1/admin/users` | Listar todos los usuarios |
| `GET` | `/api/v1/admin/users/{userId}` | Detalle de usuario |
| `PATCH` | `/api/v1/admin/users/{userId}/block` | Bloquear usuario |
| `PATCH` | `/api/v1/admin/users/{userId}/unblock` | Desbloquear usuario |
| `DELETE` | `/api/v1/admin/users/{userId}` | Eliminar usuario (soft delete) |
| `GET` | `/api/v1/admin/vehicles` | Listar todos los vehículos |
| `GET` | `/api/v1/admin/vehicles/{vehicleId}` | Detalle de vehículo |
| `PATCH` | `/api/v1/admin/vehicles/{vehicleId}/transfer-ownership` | Transferir propiedad |

---

> 📌 **Nota para desarrolladores frontend/móvil:**  
> - Todos los campos de tipo `LocalDate` se envían y reciben como `"yyyy-MM-dd"` (ej: `"2026-03-15"`).  
> - Todos los campos de tipo `LocalTime` se envían y reciben como `"HH:mm"` (ej: `"08:00"`).  
> - Todos los campos de tipo `LocalDateTime` se reciben como `"yyyy-MM-ddTHH:mm:ss"` (ej: `"2026-03-15T10:30:00"`).  
> - Los enums se envían como strings en UPPER_CASE (ej: `"OIL_CHANGE"`, `"ADMIN"`, `"MECANICO"`).  
> - El token JWT se envía en el header: `Authorization: Bearer <token>`.  
> - Errores de validación retornan `400` con un `details` que es un `Map<String, String>` donde la clave es el nombre del campo y el valor es el mensaje de error.

