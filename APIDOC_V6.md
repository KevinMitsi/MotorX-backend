# APIDOC V6 - MotorX (Addendum de Cambios)

> **Base funcional:** este documento complementa `APIDOC.md` + `APIDOC_V2.md` + `APIDOC_V3.md` + `APIDOC_V4.md` + `APIDOC_V5.md`.
>
> **Adicional V6:** ajusta el flujo del técnico en ordenes de servicio, integra el descuento real de inventario al agregar repuestos, recalcula totales y agrega el envio del detalle de servicio por correo en HTML.
>
> **Fecha de consolidacion:** 2026-05-20

---

## 1) Resumen de cambios respecto a V5

1. **Inventario conectado al flujo del tecnico**: al agregar repuestos a una orden, el stock se descuenta usando el servicio de inventario ya existente.
2. **Calculo de totales de orden**: se consolida el total de procedimientos, total de repuestos y total a pagar.
3. **Envio de detalle de servicio por correo**: nuevo endpoint para enviar al cliente un correo HTML con el cobro ordenado en tablas.
4. **Plantilla HTML propia**: se agrega una plantilla de correo para el detalle de servicio.

---

## 2) Flujo funcional actualizado

### 2.1 Agregar repuestos a una orden

- El tecnico selecciona un repuesto desde inventario.
- El sistema valida stock suficiente.
- El sistema descuenta el stock usando el servicio de inventario existente.
- Si no hay stock, responde con `422`.
- Si el repuesto ya estaba en la orden, solo incrementa la cantidad de la linea en la orden.

### 2.2 Calculo de totales

- `totalServices` = suma de los costos ingresados por el tecnico en los procedimientos de la orden.
- `totalSpareParts` = suma de `unitPrice * quantity` para cada repuesto agregado.
- `totalToPay` = `totalServices + totalSpareParts`.

### 2.3 Envio del detalle de servicio

- El tecnico puede enviar el detalle de la orden al correo del usuario dueño de la motocicleta.
- El correo se renderiza en HTML con tablas para:
  - metadatos de la orden
  - procedimientos
  - repuestos
  - resumen final de cobro

---

## 3) Endpoint nuevo

### 3.1 Ordenes de servicio

Base path: `/api/v1/orders`

| Metodo | Endpoint | Descripcion | Request DTO | Response DTO | Acceso |
|---|---|---|---|---|---|
| `POST` | `/api/v1/orders/{orderId}/send-service-details` | Enviar por correo el detalle del servicio en HTML | - | - | `TECHNICIAN` |

### 3.2 Comportamiento

- Busca la orden por ID.
- Toma el correo del usuario asociado a la motocicleta.
- Rellena la plantilla `service-details.html`.
- Envía el correo con:
  - datos de orden
  - procedimientos
  - repuestos
  - totales finales

---

## 4) Plantilla HTML nueva

Archivo:

- `src/main/resources/static/emails/service-details.html`

### 4.1 Placeholders usados

- `{{CLIENT_NAME}}`
- `{{CLIENT_EMAIL}}`
- `{{ORDER_ID}}`
- `{{APPOINTMENT_ID}}`
- `{{TECHNICIAN_NAME}}`
- `{{VEHICLE_INFO}}`
- `{{ORDER_STATUS}}`
- `{{ORDER_DATE}}`
- `{{PROCEDURE_ROWS}}`
- `{{SPARE_ROWS}}`
- `{{TOTAL_SERVICES}}`
- `{{TOTAL_SPARE_PARTS}}`
- `{{TOTAL_TO_PAY}}`

---

## 5) Integracion con inventario

### 5.1 Repuestos

- La orden llama al flujo de venta de inventario existente.
- El descuento de stock ya no se calcula manualmente en la orden.
- El inventario conserva su propia transaccion y validacion.

### 5.2 Reglas

- Si el stock no alcanza, la operacion falla.
- La orden y el inventario quedan consistentes por transaccion.

---

## 6) DTOs y respuestas afectadas

### 6.1 `OrderResponseDTO`

Sigue devolviendo:

- `id`
- `appointmentId`
- `employeeId`
- `startDate`
- `endDate`
- `totalServices`
- `totalSpareParts`
- `totalToPay`
- `status`
- `procedures`
- `spares`

### 6.2 `OrderSpareResponseDTO`

- `lineTotal` representa `unitPrice * quantity`.

---

## 7) Seguridad

- El endpoint `send-service-details` requiere rol `TECHNICIAN`.
- Mantiene las mismas reglas de seguridad del modulo de ordenes.

---

## 8) Notas de implementacion

- El correo usa HTML propio con tablas.
- El contenido se genera desde la orden ya consolidada.
- El envio es reutilizable desde el servicio de notificaciones por plantillas.

> `APIDOC_V6.md` es incremental y no reemplaza la documentacion previa.

