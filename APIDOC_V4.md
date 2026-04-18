# APIDOC V4 - MotorX (Addendum de Cambios)

> **Base funcional:** Esta version incluye todo lo documentado en `APIDOC.md`, `APIDOC_V2.md` y `APIDOC_V3.md`.
>
> **Adicional V4:** Este documento describe los cambios de auditoria para inventario/recepcion/repuestos y el nuevo modulo de notificaciones internas.
>
> **Fecha de consolidacion:** 2026-04-15

---

## 1) Resumen de cambios respecto a V3

En esta version se incorporan 2 bloques principales:

1. **Auditoria (logs) extendida** para:
   - `SpareServiceImpl`
   - `InventoryTransactionServiceImpl`
   - `ReceptionServiceImpl`
2. **Nuevo modulo de notificaciones internas por usuario**:
   - `NotificationEntity`
   - `JpaNotificationRepository`
   - `INotificationService` / `NotificationServiceImpl`
   - `NotificationController`

Tambien se completaron anotaciones Swagger en controladores de inventario, repuestos y recepcion.

---

## 2) Cambios en sistema de Logs

### 2.1 Nuevos `LogServiceName`

- `SPARE`
- `INVENTORY`
- `RECEPTION`
- `NOTIFICATION`

### 2.2 Nuevos `LogActionType`

- `CREATE_SPARE`
- `UPDATE_SPARE`
- `UPDATE_SPARE_PURCHASE_PRICE`
- `DELETE_SPARE`
- `REGISTER_PURCHASE`
- `REGISTER_SALE`
- `INITIATE_RECEPTION`
- `CONFIRM_RECEPTION`
- `CREATE_NOTIFICATION`
- `READ_NOTIFICATION`
- `READ_ALL_NOTIFICATIONS`

### 2.3 Servicios instrumentados

- **Repuestos (`SpareServiceImpl`)**
  - Log de exito/fallo en crear, actualizar, actualizar precio y eliminar.
- **Inventario (`InventoryTransactionServiceImpl`)**
  - Log de exito/fallo en registrar compras y ventas.
- **Recepcion (`ReceptionServiceImpl`)**
  - Log de exito/fallo al iniciar y confirmar recepcion.

---

## 3) Nuevo modulo: Notificaciones internas

### 3.1 Modelo de datos

Nueva entidad: `NotificationEntity`

Campos principales:
- `id`
- `user` (FK a `users`)
- `title`
- `description`
- `urgency` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`)
- `isRead`
- `readAt`
- `source`
- `createdAt`

Relación funcional: **un usuario puede tener muchas notificaciones**.

### 3.2 Repositorio

- `JpaNotificationRepository`
  - `findByUserIdOrderByCreatedAtDesc(...)`
  - `findByUserIdAndIsReadFalseOrderByCreatedAtDesc(...)`
  - `findByIdAndUserId(...)`

### 3.3 DTOs nuevos

- `CreateNotificationDTO`
- `NotificationResponseDTO`
- `MarkAllNotificationsReadResponseDTO`

### 3.4 Servicio

`NotificationServiceImpl` implementa:
- Creacion de notificaciones (admin)
- Consulta de notificaciones propias (todas o solo no leidas)
- Consulta administrativa por usuario
- Marcar una notificacion como leida
- Marcar todas como leidas

Incluye auditoria de exito/fallo para acciones del modulo.

### 3.5 Excepcion nueva

- `NotificationNotFoundException` -> `404` en `GlobalControllerAdvice`

---

## 4) Endpoints nuevos de Notificaciones

Base path: `/api/v1/notifications`

| Metodo | Endpoint | Descripcion | Acceso |
|---|---|---|---|
| `POST` | `/api/v1/notifications/admin` | Crear notificacion para un usuario | Solo `ADMIN` |
| `GET` | `/api/v1/notifications/my` | Listar mis notificaciones (`onlyUnread` opcional) | Autenticado |
| `PATCH` | `/api/v1/notifications/my/{notificationId}/read` | Marcar una notificacion como leida | Autenticado |
| `PATCH` | `/api/v1/notifications/my/read-all` | Marcar todas mis notificaciones como leidas | Autenticado |
| `GET` | `/api/v1/notifications/admin/user/{userId}` | Listar notificaciones de un usuario | Solo `ADMIN` |

---

## 5) Seguridad

En `SecurityConfig` se agrega ruta explicita:

- `/api/v1/notifications/**` -> autenticado

Controles finos por endpoint en controlador con `@PreAuthorize`:
- operaciones administrativas solo `ADMIN`
- operaciones de bandeja personal para cualquier usuario autenticado

---

## 6) Swagger / OpenAPI

Se amplió documentacion en:
- `SpareController`
- `InventoryTransactionController`
- `ReceptionController`
- `NotificationController` (nuevo)

Cobertura de:
- `@Operation`
- `@ApiResponses`
- `@Parameter`
- `@Schema` en DTOs relevantes

---

## 7) Persistencia y migraciones

Nueva migracion:

- `V10__extend_logs_and_add_notifications.sql`

Incluye:
1. Extension de constraints de `logs` para nuevos servicios y acciones.
2. Creacion de tabla `notifications` con FK a `users`.
3. Indices por usuario/fecha y usuario/estado de lectura.

---

## 8) Pruebas agregadas/actualizadas

### 8.1 Servicios
- `SpareServiceImplTest` (actualizado con validaciones de logs)
- `InventoryTransactionServiceImplTest` (actualizado con validaciones de logs)
- `ReceptionServiceImplTest` (actualizado con validaciones de logs)
- `NotificationServiceImplTest` (nuevo)

### 8.2 Controllers
- `NotificationControllerTest` (nuevo)

---

## 9) Impacto para frontend

Nuevas capacidades para UI:
1. Bandeja de notificaciones por usuario.
2. Filtro rapido de no leidas (`onlyUnread=true`).
3. Accion de marcar una o todas como leidas.
4. Vista administrativa para soporte/seguimiento por usuario.

Compatibilidad:
- Se mantiene formato de errores `ResponseErrorDTO`.
- Se extiende catalogo de enums de auditoria (logs).

---

## 10) Extension de inventario (stock threshold)

Se agrega el campo `stockThreshold` a repuestos para definir minimo de existencias antes de alerta.

### Cambios principales

1. `Spare` agrega `stockThreshold` (entero >= 0).
2. DTOs actualizados:
   - `CreateSpareDTO`
   - `UpdateSpareDTO`
   - `SpareResponseDTO`
3. Nuevo endpoint:
   - `GET /api/v1/spares/below-threshold` para listar repuestos bajo umbral.
4. Nueva accion admin:
   - `POST /api/v1/spares/{id}/notify-restock` para notificar a empleados `WAREHOUSE_WORKER` con mensaje de surtido y codigo de estanteria.

### Notificacion automatica critica

En registro de ventas (`InventoryTransactionServiceImpl`), cuando un repuesto queda con `quantity < stockThreshold`, se envía notificacion `CRITICAL` a usuarios `ADMIN`.

### Migracion

- `V11__add_spare_stock_threshold.sql`

### Endpoints agregados en esta extension

| Metodo | Endpoint | Descripcion | Acceso |
|---|---|---|---|
| `GET` | `/api/v1/spares/below-threshold` | Lista repuestos bajo umbral | `ADMIN`, `EMPLOYEE` |
| `POST` | `/api/v1/spares/{id}/notify-restock` | Notifica surtido a personal de bodega | Solo `ADMIN` |

---

## 11) Extension de consultas y metricas de inventario

### 11.1 Busqueda de repuestos (modulo Spares)

Se extiende `GET /api/v1/spares` para soportar filtros opcionales:

- `name` (string, opcional): coincidencia parcial por nombre.
- `savCode` (string, opcional): coincidencia parcial por codigo SAV.

Si no se envian filtros, el endpoint conserva el comportamiento actual de listado completo.

### 11.2 Nuevas metricas de inventario (modulo Admin Metrics)

Se agregan endpoints bajo `/api/v1/admin/metrics/inventory`:

| Metodo | Endpoint | Descripcion | Parametros | Acceso |
|---|---|---|---|---|
| `GET` | `/api/v1/admin/metrics/inventory/top-selling` | Ranking de repuestos mas vendidos | `limit` (opcional, default `10`) | Solo `ADMIN` |
| `GET` | `/api/v1/admin/metrics/inventory/profit` | Ventas brutas y ganancia estimada por rango de fechas | `startDate`, `endDate` (`yyyy-MM-dd`) | Solo `ADMIN` |
| `GET` | `/api/v1/admin/metrics/inventory/stagnant` | Repuestos estancados (sin venta reciente o nunca vendidos) | `daysWithoutSales` (opcional, default `60`) | Solo `ADMIN` |
| `GET` | `/api/v1/admin/metrics/inventory/below-threshold-percentage` | Porcentaje de repuestos por debajo del umbral | Sin parametros | Solo `ADMIN` |

Notas de calculo:
- Ganancia estimada usa los margenes definidos del dominio: `35%` (repuesto normal) y `25%` (aceites).
- El porcentaje bajo umbral solo considera repuestos con `stockThreshold > 0`.

### 11.3 Pruebas actualizadas

- `SpareControllerTest` y `SpareServiceImplTest`: cobertura de busqueda opcional por `name`/`savCode`.
- `AdminMetricsControllerTest`, `MetricsServiceImplTest` y `MetricsDtoTest`: cobertura de las nuevas metricas de inventario.

---

> `APIDOC_V4.md` funciona como addendum incremental y no reemplaza los documentos anteriores.

