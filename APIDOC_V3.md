# APIDOC V3 - MotorX (Addendum de Cambios)

> **Base funcional:** Esta version incluye todo lo documentado en `APIDOC.md` y `APIDOC_V2.md`.
>
> **Adicional V3:** Este documento describe exclusivamente los cambios nuevos incorporados para inventario de repuestos y flujo de recepcion de motos.
>
> **Fecha de consolidacion:** 2026-04-03

---

## 1) Resumen de cambios respecto a V2

En esta version se agregan 3 bloques funcionales:

1. **Nuevo tipo de empleado:** `WAREHOUSE_WORKER` (en `EmployeePosition`).
2. **Modulo de inventario:**
   - Catalogo de repuestos (`Spare`).
   - Transacciones de compra (`PurchaseTransaction` + items).
   - Transacciones de venta (`SaleTransaction` + items).
3. **Flujo de recepcion de motos:**
   - Inicio de recepcion con codigo de 4 digitos enviado por correo.
   - Confirmacion de recepcion por placa + codigo para pasar la cita a `IN_PROGRESS`.

> Importante: se mantiene el esquema de roles de usuario (`CLIENT`, `EMPLOYEE`, `ADMIN`).
> El perfil de bodega se controla por **tipo de empleado** (`EmployeePosition.WAREHOUSE_WORKER`), no por un rol JWT nuevo.

---

## 2) Endpoints nuevos

### 2.1 Repuestos - `/api/v1/spares`

| Metodo | Endpoint | Descripcion | Acceso |
|---|---|---|---|
| `POST` | `/api/v1/spares` | Crear repuesto | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/spares` | Listar repuestos | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/spares/{id}` | Consultar repuesto por ID | `ADMIN`, `EMPLOYEE` |
| `PUT` | `/api/v1/spares/{id}` | Actualizar repuesto completo | `ADMIN`, `EMPLOYEE` |
| `PATCH` | `/api/v1/spares/{id}/purchase-price` | Actualizar solo precio de compra | `ADMIN`, `EMPLOYEE` |
| `DELETE` | `/api/v1/spares/{id}` | Eliminar repuesto | Solo `ADMIN` |

### 2.2 Inventario - Transacciones - `/api/v1/inventory`

| Metodo | Endpoint | Descripcion | Acceso |
|---|---|---|---|
| `POST` | `/api/v1/inventory/purchases` | Registrar compra (entrada) | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/inventory/purchases` | Listar compras | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/inventory/purchases/{id}` | Detalle de compra | `ADMIN`, `EMPLOYEE` |
| `POST` | `/api/v1/inventory/sales` | Registrar venta (salida) | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/inventory/sales` | Listar ventas | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/inventory/sales/today` | Resumen de ventas del dia | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/inventory/sales/{id}` | Detalle de venta | `ADMIN`, `EMPLOYEE` |

### 2.3 Recepcion de motos - `/api/v1/reception`

| Metodo | Endpoint | Descripcion | Acceso |
|---|---|---|---|
| `POST` | `/api/v1/reception/initiate/{appointmentId}` | Inicia recepcion, genera/en via codigo de 4 digitos y pasa cita a `AWAITING_CONFIRMATION` | `ADMIN`, `EMPLOYEE` |
| `POST` | `/api/v1/reception/confirm` | Confirma recepcion con placa + codigo y pasa cita a `IN_PROGRESS` | `ADMIN`, `EMPLOYEE` |

---

## 3) DTOs nuevos

### 3.1 Inventario - Repuestos

- `CreateSpareDTO`
- `UpdateSpareDTO`
- `UpdateSparePurchasePriceDTO`
- `SpareResponseDTO`

Campos clave:
- codigos `savCode` y `spareCode` (unicidad)
- `purchasePriceWithVat` (`BigDecimal`)
- `isOil`
- `warehouseLocation` con formato `00-00-00-00`
- `salePrice` calculado en respuesta (no persistido)

### 3.2 Inventario - Compras

- `CreatePurchaseTransactionDTO`
- `CreatePurchaseItemDTO`
- `PurchaseTransactionResponseDTO`
- `PurchaseItemResponseDTO`

### 3.3 Inventario - Ventas

- `CreateSaleTransactionDTO`
- `CreateSaleItemDTO`
- `SaleTransactionResponseDTO`
- `SaleItemResponseDTO`
- `DailySalesSummaryDTO`

### 3.4 Recepcion

- `ConfirmReceptionDTO`

---

## 4) Cambios de dominio y reglas de negocio

### 4.1 Nuevas entidades

- `Spare`
- `PurchaseTransaction`
- `PurchaseTransactionItem`
- `SaleTransaction`
- `SaleTransactionItem`

### 4.2 Entidades existentes modificadas

- `EmployeePosition`: agrega `WAREHOUSE_WORKER`
- `AppointmentStatus`: agrega `AWAITING_CONFIRMATION`
- `AppointmentEntity`: agrega
  - `verificationCode` (4 digitos)
  - `verificationCodeCreatedAt`

### 4.3 Reglas implementadas

- **Precio de venta calculado (no persistido):**
  - `isOil = true` -> margen `25%`
  - `isOil = false` -> margen `35%`
- **Validacion de ubicacion de bodega:** regex `\d{2}-\d{2}-\d{2}-\d{2}`.
- **Compra de inventario:**
  - incrementa `Spare.quantity`
  - actualiza `Spare.purchasePriceWithVat` con el ultimo valor de compra
- **Venta de inventario:**
  - decrementa `Spare.quantity`
  - evita stock negativo (`InsufficientStockException`)
  - si se asocia cita, exige estado `IN_PROGRESS`
- **Recepcion de cita:**
  - `initiate`: estado `SCHEDULED -> AWAITING_CONFIRMATION`, genera codigo y envia email
  - `confirm`: valida placa/codigo/expiracion y mueve `AWAITING_CONFIRMATION -> IN_PROGRESS`

---

## 5) Persistencia y migraciones

### 5.1 Nueva migracion

- `V9__inventory_module.sql`

Incluye:
- ampliacion de `employees.position` con `WAREHOUSE_WORKER`
- ampliacion de `appointments.status` con `AWAITING_CONFIRMATION`
- columnas de verificacion en `appointments`
- tablas nuevas:
  - `spares`
  - `purchase_transactions`
  - `purchase_transaction_items`
  - `sale_transactions`
  - `sale_transaction_items`
- constraints e indices para codigos, cantidades, precios y consultas de transacciones

### 5.2 Nuevos repositorios

- `JpaSpareRepository`
- `JpaPurchaseTransactionRepository`
- `JpaSaleTransactionRepository`

Y ampliacion de `JpaAppointmentRepository` para consultas de recepcion por placa/estado.

---

## 6) Servicios y mappers nuevos

### 6.1 Servicios

- `ISpareService` / `SpareServiceImpl`
- `IInventoryTransactionService` / `InventoryTransactionServiceImpl`
- `IReceptionService` / `ReceptionServiceImpl`

### 6.2 Mappers

- `SpareMapper`
- `PurchaseTransactionMapper`
- `SaleTransactionMapper`

---

## 7) Excepciones nuevas y manejo global

Se agregan excepciones de dominio y sus handlers en `GlobalControllerAdvice`:

- `SpareNotFoundException` -> `404`
- `DuplicateSpareCodeException` -> `409`
- `InsufficientStockException` -> `422`
- `InvalidWarehouseLocationException` -> `400`
- `AppointmentNotInProcessException` -> `422`
- `InvalidVerificationCodeException` -> `400`
- `AppointmentNotEligibleForReceptionException` -> `422`

---

## 8) Cambios de seguridad

Se agregaron reglas para nuevas rutas en `SecurityConfig`:

- `/api/v1/spares/**`
- `/api/v1/inventory/**`
- `/api/v1/reception/**`

Acceso por rol de usuario:
- `ADMIN` y/o `EMPLOYEE` segun endpoint.

Autorizacion funcional por tipo de empleado (en servicio):
- Compras y operaciones de bodega: `EmployeePosition.WAREHOUSE_WORKER` o `ADMIN`
- Registro de ventas: `EmployeePosition.RECEPCIONISTA` o `ADMIN`

---

## 9) Resumen de impacto para frontend

Nuevos modulos disponibles:

1. **Inventario de repuestos:** CRUD + precio de venta calculado.
2. **Entradas de inventario:** registro de compras con detalle por item.
3. **Salidas de inventario:** registro de ventas, detalle historico por item y resumen diario.
4. **Recepcion de motos:** flujo de doble paso con codigo de 4 digitos.

Compatibilidad:

- Se mantiene formato de errores `ResponseErrorDTO`.
- Se conserva el esquema de roles JWT de versiones anteriores.
- Se amplian enums (`EmployeePosition`, `AppointmentStatus`) que deben contemplarse en clientes.

---

## 10) Endpoints a incorporar al resumen global

| Metodo | Endpoint | Acceso |
|---|---|---|
| `POST` | `/api/v1/spares` | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/spares` | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/spares/{id}` | `ADMIN`, `EMPLOYEE` |
| `PUT` | `/api/v1/spares/{id}` | `ADMIN`, `EMPLOYEE` |
| `PATCH` | `/api/v1/spares/{id}/purchase-price` | `ADMIN`, `EMPLOYEE` |
| `DELETE` | `/api/v1/spares/{id}` | Solo `ADMIN` |
| `POST` | `/api/v1/inventory/purchases` | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/inventory/purchases` | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/inventory/purchases/{id}` | `ADMIN`, `EMPLOYEE` |
| `POST` | `/api/v1/inventory/sales` | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/inventory/sales` | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/inventory/sales/today` | `ADMIN`, `EMPLOYEE` |
| `GET` | `/api/v1/inventory/sales/{id}` | `ADMIN`, `EMPLOYEE` |
| `POST` | `/api/v1/reception/initiate/{appointmentId}` | `ADMIN`, `EMPLOYEE` |
| `POST` | `/api/v1/reception/confirm` | `ADMIN`, `EMPLOYEE` |

---

> Este `APIDOC_V3.md` funciona como complemento de `APIDOC.md` y `APIDOC_V2.md`: no reemplaza la documentacion anterior, la extiende con los cambios de inventario y recepcion.

