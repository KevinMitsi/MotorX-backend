# APIDOC V5 - MotorX (Addendum de Cambios)

> **Base funcional:** Esta version incluye todo lo documentado en `APIDOC.md`, `APIDOC_V2.md`, `APIDOC_V3.md` y `APIDOC_V4.md`.
>
> **Adicional V5:** Este documento describe el nuevo catalogo de procedimientos, la gestion de ordenes de servicio, la relacion servicio-procedimiento, seguridad para rol TECHNICIAN y extension de logs.
>
> **Fecha de consolidacion:** 2026-05-04

---

## 1) Resumen de cambios respecto a V4

En esta version se incorporan 4 bloques principales:

1. **Nuevo catalogo de procedimientos** con CRUD basico y flag `active`.
2. **Relacion servicios - procedimientos base** para precargar procedimientos por tipo de servicio.
3. **Flujo de ordenes de servicio**: crear orden por cita, agregar procedimientos y repuestos, recalcular totales y completar orden.
4. **Seguridad y auditoria**: rol `TECHNICIAN`, rutas protegidas y nuevas acciones de log para ordenes de servicio.
5. **Catalogo de servicios del taller (admin)**: CRUD de servicios y gestion de procedimientos base por servicio.

---

## 2) Modelo de datos y relaciones

### 2.1 Nuevas tablas

- `procedures`
  - `id`, `name` (unique), `description`, `active`, `created_at`, `updated_at`.
- `service_procedures` (N:M)
  - `service_id`, `procedure_id`.
- `order_procedures`
  - `order_id`, `procedure_id`, `cost`.
- `order_spares`
  - `order_id`, `spare_id`, `quantity`, `unit_price`.

### 2.2 Entidades agregadas

- `ProcedureEntity`
- `OrderServiceEntity` (usa `service_orders` existente)
- `OrderProcedureEntity` + `OrderProcedureId`
- `OrderSpareEntity` + `OrderSpareId`

---

## 3) DTOs nuevos

### 3.1 Ordenes de servicio

- `AddProcedureToOrderDTO`
  - `procedureId` (required)
  - `cost` (required, >= 0)
- `AddSpareToOrderDTO`
  - `spareId` (required)
  - `quantity` (required, >= 1)
- `UpdateOrderProcedureCostDTO`
  - `cost` (required, >= 0)
- `OrderResponseDTO`
  - `id`, `appointmentId`, `employeeId`, `startDate`, `endDate`
  - `totalServices`, `totalSpareParts`, `totalToPay`, `status`
  - `procedures: List<OrderProcedureResponseDTO>`
  - `spares: List<OrderSpareResponseDTO>`
- `OrderProcedureResponseDTO`
  - `procedureId`, `procedureName`, `cost`
- `OrderSpareResponseDTO`
  - `spareId`, `spareName`, `quantity`, `unitPrice`, `lineTotal`
- `TechnicianDailyOrderDTO`
  - `appointmentId`, `orderId`
  - `licensePlate`, `brand`, `model`
  - `appointmentDate`, `startTime`, `processStartedAt`

### 3.2 Procedimientos

- `CreateProcedureDTO`
  - `name` (required, max 150)
  - `description` (max 1000)
  - `active` (opcional, default `true`)
- `UpdateProcedureDTO`
  - `name` (required, max 150)
  - `description` (max 1000)
  - `active` (opcional)
- `ProcedureResponseDTO`
  - `id`, `name`, `description`, `active`, `createdAt`, `updatedAt`
- `UpdateServiceProceduresDTO`
  - `procedureIds` (required, lista de IDs)

### 3.3 Servicios del taller

- `CreateServiceDTO`
  - `name` (required, max 150)
  - `description` (max 1000)
  - `estimatedDurationMinutes` (required, >= 1)
  - `basePrice` (required, >= 0)
  - `active` (opcional, default `true`)
  - `procedureIds` (opcional, lista de IDs)
- `UpdateServiceDTO`
  - `name` (required, max 150)
  - `description` (max 1000)
  - `estimatedDurationMinutes` (required, >= 1)
  - `basePrice` (required, >= 0)
  - `active` (opcional)
- `ServiceResponseDTO`
  - `id`, `name`, `description`, `estimatedDurationMinutes`, `basePrice`, `active`
  - `baseProcedures: List<ProcedureResponseDTO>`
  - `createdAt`, `updatedAt`

---

## 4) Modulo: Procedimientos

### 4.1 Comportamiento principal

- El nombre de procedimiento es **unico**.
- `active` se mantiene en `true` si no se envia en creacion.
- Se puede consultar:
  - lista completa
  - lista de activos
  - por ID
  - procedimientos base por servicio
- Se puede actualizar la lista de procedimientos base de un servicio reemplazando el set completo.

---

## 5) Modulo: Ordenes de servicio

### 5.1 Creacion de orden

- Se crea desde una cita (`appointmentId`).
- La cita debe estar en estado `IN_PROGRESS`.
- El tecnico autenticado debe estar asignado a la cita.
- Si la orden ya existe para la cita, se retorna la existente (operacion idempotente).

### 5.2 Agregar procedimientos

- No se permiten procedimientos duplicados en la misma orden.
- Se guarda el costo del procedimiento dentro de la orden.
- Se recalculan totales luego de agregar.

### 5.3 Actualizar costo de procedimiento

- Requiere que la orden sea editable (no `COMPLETED` ni `CANCELLED`).
- Recalcula totales.

### 5.4 Agregar repuestos

- Valida stock suficiente (`quantity` disponible).
- Se descuenta el stock del repuesto.
- `unitPrice` se calcula asi:
  - Si `isOil = true` -> `purchasePriceWithVat * 1.25`.
  - Si `isOil = false` -> `purchasePriceWithVat * 1.35`.
- Si el repuesto ya existe en la orden, incrementa la cantidad.
- Recalcula totales.

### 5.5 Completar orden

- Marca `status = COMPLETED` y asigna `endDate`.

### 5.6 Recalculo de totales

- `totalServices` = suma de costos de procedimientos.
- `totalSpareParts` = suma de `unitPrice * quantity`.
- `totalToPay` = `totalServices + totalSpareParts`.

### 5.7 Citas del tecnico (listado diario)

- El endpoint `GET /api/v1/orders/my/today` considera "hoy" segun `processStartedAt` (fecha/hora de confirmacion de recepcion).

### 5.8 Servicios del taller (admin)

- CRUD completo de servicios del taller (`/api/v1/services`).
- Los procedimientos base se gestionan por servicio (reemplazo de set completo).

---

## 6) Endpoints nuevos

### 6.1 Procedimientos

Base path: `/api/v1/procedures`

| Metodo | Endpoint | Descripcion | Request DTO | Response DTO | Acceso |
|---|---|---|---|---|---|
| `POST` | `/api/v1/procedures` | Crear procedimiento | `CreateProcedureDTO` | `ProcedureResponseDTO` | `ADMIN` |
| `GET` | `/api/v1/procedures` | Listar procedimientos | - | `List<ProcedureResponseDTO>` | `ADMIN`, `TECHNICIAN` |
| `GET` | `/api/v1/procedures/active` | Listar procedimientos activos | - | `List<ProcedureResponseDTO>` | `ADMIN`, `TECHNICIAN` |
| `GET` | `/api/v1/procedures/{id}` | Consultar procedimiento por ID | - | `ProcedureResponseDTO` | `ADMIN`, `TECHNICIAN` |
| `PUT` | `/api/v1/procedures/{id}` | Actualizar procedimiento | `UpdateProcedureDTO` | `ProcedureResponseDTO` | `ADMIN` |
| `GET` | `/api/v1/procedures/service/{serviceId}` | Listar procedimientos base de un servicio | - | `List<ProcedureResponseDTO>` | `ADMIN`, `TECHNICIAN` |
| `PUT` | `/api/v1/procedures/service/{serviceId}` | Actualizar procedimientos base de un servicio | `UpdateServiceProceduresDTO` | `List<ProcedureResponseDTO>` | `ADMIN` |

### 6.2 Ordenes de servicio

Base path: `/api/v1/orders`

| Metodo | Endpoint | Descripcion | Request DTO | Response DTO | Acceso |
|---|---|---|---|---|---|
| `POST` | `/api/v1/orders/appointment/{appointmentId}` | Crear orden para una cita | - | `OrderResponseDTO` | `TECHNICIAN` |
| `GET` | `/api/v1/orders/appointment/{appointmentId}` | Consultar orden por cita | - | `OrderResponseDTO` | `ADMIN`, `TECHNICIAN` |
| `POST` | `/api/v1/orders/{orderId}/procedures` | Agregar procedimiento a una orden | `AddProcedureToOrderDTO` | `OrderResponseDTO` | `TECHNICIAN` |
| `PATCH` | `/api/v1/orders/{orderId}/procedures/{procedureId}` | Actualizar costo de procedimiento en una orden | `UpdateOrderProcedureCostDTO` | `OrderResponseDTO` | `TECHNICIAN` |
| `POST` | `/api/v1/orders/{orderId}/spares` | Agregar repuesto a una orden | `AddSpareToOrderDTO` | `OrderResponseDTO` | `TECHNICIAN` |
| `POST` | `/api/v1/orders/{orderId}/complete` | Completar una orden | - | `OrderResponseDTO` | `TECHNICIAN` |
| `GET` | `/api/v1/orders/my/today` | Listar citas con recepcion confirmada hoy del tecnico autenticado | - | `List<TechnicianDailyOrderDTO>` | `TECHNICIAN` |

### 6.3 Servicios del taller (admin)

Base path: `/api/v1/services`

| Metodo | Endpoint | Descripcion | Request DTO | Response DTO | Acceso |
|---|---|---|---|---|---|
| `POST` | `/api/v1/services` | Crear servicio | `CreateServiceDTO` | `ServiceResponseDTO` | `ADMIN` |
| `GET` | `/api/v1/services` | Listar servicios | - | `List<ServiceResponseDTO>` | `ADMIN` |
| `GET` | `/api/v1/services/{id}` | Consultar servicio por ID | - | `ServiceResponseDTO` | `ADMIN` |
| `PUT` | `/api/v1/services/{id}` | Actualizar servicio | `UpdateServiceDTO` | `ServiceResponseDTO` | `ADMIN` |
| `DELETE` | `/api/v1/services/{id}` | Eliminar servicio | - | - | `ADMIN` |
| `GET` | `/api/v1/services/{id}/procedures` | Listar procedimientos base de un servicio | - | `List<ProcedureResponseDTO>` | `ADMIN` |
| `PUT` | `/api/v1/services/{id}/procedures` | Actualizar procedimientos base de un servicio | `UpdateServiceProceduresDTO` | `List<ProcedureResponseDTO>` | `ADMIN` |

---

## 7) Excepciones y casos

### 7.1 Excepciones nuevas (mapeadas en `GlobalControllerAdvice`)

- `ServiceNotFoundException` -> `404` cuando `serviceId` no existe.
- `ProcedureNotFoundException` -> `404` cuando `procedureId` no existe o no pertenece a la orden.
- `OrderServiceNotFoundException` -> `404` cuando la orden no existe.
- `DuplicateProcedureNameException` -> `409` cuando se intenta crear/actualizar con nombre duplicado.
- `TechnicianNotAssignedException` -> `403` si el tecnico autenticado no esta asignado a la cita u orden.
- `DuplicateServiceNameException` -> `409` cuando se intenta crear/actualizar con nombre duplicado.

### 7.2 Excepciones reutilizadas relevantes

- `AppointmentNotFoundException` -> `404` al crear orden si la cita no existe.
- `AppointmentNotInProcessException` -> `422` si la cita no esta en `IN_PROGRESS`.
- `EmployeeNotFoundException` -> `404` si el usuario autenticado no es empleado.
- `SpareNotFoundException` -> `404` al agregar repuesto no existente.
- `InsufficientStockException` -> `422` si el stock no alcanza para el repuesto.
- `IllegalArgumentException` -> `400` si el procedimiento ya esta en la orden o por validacion de fechas.
- `IllegalStateException` -> `409` si la orden esta `COMPLETED` o `CANCELLED` y se intenta modificar.

### 7.3 Codigos de error estandar

- `401` No autenticado.
- `403` Sin permisos.
- `400` Validaciones o argumentos invalidos.
- `404` Recurso no encontrado.
- `409` Conflicto de estado o nombre duplicado.
- `422` Regla de negocio no cumplida.

---

## 8) Seguridad

Cambios en `SecurityConfig`:

- Se define el rol `TECHNICIAN`.
- Rutas protegidas:
  - `/api/v1/orders/**` -> `ADMIN` o `TECHNICIAN` (ademas del `@PreAuthorize` por endpoint).
  - `GET /api/v1/procedures/**` -> `ADMIN` o `TECHNICIAN`.
  - `POST`/`PUT /api/v1/procedures/**` -> solo `ADMIN`.
  - `/api/v1/services/**` -> solo `ADMIN`.

---

## 9) Logs y auditoria

Se agregan nuevos valores a los enums de auditoria:

### 9.1 `LogServiceName`

- `SERVICE_ORDER`

### 9.2 `LogActionType`

- `CREATE_SERVICE_ORDER`
- `ADD_ORDER_PROCEDURE`
- `UPDATE_ORDER_PROCEDURE`
- `ADD_ORDER_SPARE`
- `COMPLETE_SERVICE_ORDER`

El servicio de ordenes registra **exito y fallo** en cada operacion con el usuario autenticado.

---

## 10) Migraciones

### 10.1 V13

`V13__service_order_procedures.sql` incluye:

1. Creacion de `procedures`.
2. Relacion `service_procedures` (N:M).
3. Creacion de `order_procedures`.
4. Creacion de `order_spares`.

### 10.2 V14

`V14__extend_logs_for_service_orders.sql` incluye:

1. Extension de constraints de `logs` para `SERVICE_ORDER`.
2. Extension de constraints para nuevas acciones de ordenes de servicio.

---

## 11) Swagger / OpenAPI

Se agregaron anotaciones de Swagger completas en:

- `OrderServiceController`
- `ProcedureController`
- `OurServicesController`

Cobertura de:

- `@Operation`
- `@ApiResponses`
- `@Parameter`
- `@Schema` en DTOs de error

> `APIDOC_V5.md` funciona como addendum incremental y no reemplaza los documentos anteriores.
