# Spec — Notifications (Pedidos STOCK)

## Purpose

Definir el sistema de notificaciones internas disparadas por pedidos
`OrderType.STOCK` (express). Cubre disparadores, destinatarios, canales,
persistencia, resiliencia y operación. Define los invariantes que el código
debe respetar.

## Requirements

### R1. Disparador (`ORDER_STOCK_CREATED`)

Cuando se persiste un pedido con `OrderType.STOCK`, el sistema publica un
`OrderNotificationEvent` con `trigger = ORDER_STOCK_CREATED`.

- El evento se procesa **después** del commit de la transacción
  (`@TransactionalEventListener(AFTER_COMMIT)`).
- El procesamiento es **asíncrono** (`@Async("notificationExecutor")`), de
  modo que el endpoint de crear pedido retorna `201 Created` sin esperar
  los envíos.
- Si el guardado falla, **no** se publica evento (consistencia).
- Si el procesamiento del listener falla tras agotar los reintentos, el
  fallo queda registrado en `notification_log` con `status = FAILED` y
  **no** afecta la consistencia del pedido.

### R2. Destinatarios (`NotificationRecipientResolver`)

Los destinatarios son **usuarios internos activos**, NO clientes. Se resuelven
desde `BusinessConfig`:

- `notifyRolesCsv` (CSV de role names): lista base. Default:
  `ROLE_ADMIN,ROLE_DISTRIBUTOR`.
- `notifyUserIdsCsv` (CSV de UUIDs): ids extra opcionales.
- Se devuelven `User` únicos (deduplicados por id) con `active = true`.
- Si un destinatario tiene `phone` o `email` nulo / inválido, ese canal
  individual se marca como `SKIPPED` (con `last_error` descriptivo) y se
  continúa con el resto. Un destinatario sin datos no aborta al resto.

### R3. Canales

#### R3.1 — WhatsApp (`NotificationChannel.WHATSAPP`)

- Proveedor: Twilio (SDK `com.twilio.sdk:twilio` v10.7.0).
- Sender por defecto: `whatsapp:+14155238886` (sandbox de Twilio en dev).
  En producción, sender propio aprobado por Meta Business Manager.
- Los envíos business-initiated requieren un **template** aprobado por Meta.
- En sandbox solo se puede enviar a números que mandaron el `join` del
  sandbox.
- Si `notifyWhatsappEnabled = false`, el canal se saltea por completo.

#### R3.2 — Email (`NotificationChannel.EMAIL`)

- Proveedor: SMTP genérico vía `spring-boot-starter-mail`.
- Dev: MailHog local (`localhost:1025`). Prod: Resend / SendGrid / SES.
- Subject y body parametrizados en `messages.properties`.
- Si `notifyEmailEnabled = false`, el canal se saltea por completo.

### R4. Persistencia (`notification_log`)

Cada intento de envío queda registrado. Un row por
`(recipient × channel × attempt)`.

```sql
CREATE TABLE notification_log (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL,
  user_id UUID,
  channel VARCHAR(20) NOT NULL,
  trigger VARCHAR(40) NOT NULL,
  recipient VARCHAR(255) NOT NULL,
  subject VARCHAR(255),
  body TEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  last_error TEXT,
  external_id VARCHAR(255),
  created_at TIMESTAMP NOT NULL,
  sent_at TIMESTAMP,
  updated_at TIMESTAMP NOT NULL
);
```

`status` ∈ `{PENDING, SENT, FAILED, SKIPPED, RETRYING}`.

### R5. Resiliencia

- **Asíncrono**: pool de hilos dedicado (`notificationExecutor`,
  `corePoolSize=4`, `maxPoolSize=8`, `queueCapacity=100`).
- **Retry**: `@Retryable` con `maxAttempts` configurable
  (default 3) y backoff exponencial (default 2000ms × multiplier 2.0).
- **Idempotencia**: el `external_id` (Twilio SID / SMTP Message-ID) se
  persiste para auditoría.
- **No bloqueante**: un fallo en el envío no impacta el flujo de creación
  del pedido.

### R6. Configuración (`BusinessConfig`)

Campos nuevos en `BusinessConfig`:

| Columna | Tipo | Default | Descripción |
|---|---|---|---|
| `notifyRolesCsv` | VARCHAR(500) | `ROLE_ADMIN,ROLE_DISTRIBUTOR` | Roles que reciben. |
| `notifyUserIdsCsv` | VARCHAR(2000) | `null` | UUIDs extra opcionales. |
| `notifyWhatsappEnabled` | BOOLEAN | `true` | Apaga WA sin redeploy. |
| `notifyEmailEnabled` | BOOLEAN | `true` | Apaga email sin redeploy. |
| `notifyOnStockOrderCreated` | BOOLEAN | `true` | Apaga el trigger entero. |

`ddl-auto: update` aplica los ALTER COLUMN en el próximo arranque.

### R7. Operación (endpoints admin `ROLE_ADMIN`)

| Método | Path | Descripción |
|---|---|---|
| `GET` | `/api/admin/notifications` | Lista paginada, filtros `status`, `orderId`, `channel`. |
| `GET` | `/api/admin/notifications/{id}` | Detalle de un intento. |
| `POST` | `/api/admin/notifications/{id}/retry` | Reintenta los `FAILED`. |
| `GET` | `/api/admin/notifications/stats` | Contadores por canal y status. |

### R8. Variables de entorno (operación)

| Var | Uso |
|---|---|
| `TWILIO_ACCOUNT_SID` | Cuenta Twilio. |
| `TWILIO_AUTH_TOKEN` | Secret Twilio (rotable). |
| `TWILIO_WHATSAPP_FROM` | Sender WA (`whatsapp:+14155238886` sandbox). |
| `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` / `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | SMTP. |
| `MAIL_FROM_ADDRESS` / `MAIL_FROM_NAME` | From del email. |
| `NOTIFY_WHATSAPP_ENABLED` / `NOTIFY_EMAIL_ENABLED` | Flags por canal. |
| `NOTIFY_RETRY_MAX_ATTEMPTS` / `NOTIFY_RETRY_INITIAL_BACKOFF_MS` | Tuning de retry. |

### R9. No regresiones

- Los pedidos `OrderType.WHOLESALE` **no** disparan notificación alguna.
- El endpoint `POST /api/orders/stock` mantiene su contrato actual de
  respuesta (`201 Created` con `OrderResponse`) — las notificaciones son
  efecto colateral, no parte del response.

---

## Estado del Sistema

- **Estado**: ✅ **Verificado y Operativo** en producción/staging (Render).
- **Validación**: Envíos de WhatsApp procesados y entregados con éxito a través de la API oficial de Twilio SDK ante la creación de pedidos `OrderType.STOCK`.
- **Resiliencia**: Captura de errores, retries automáticos con `@Retryable` e historial registrado en `notification_logs`.

---

## Guía de Configuración de Destinatarios (Gestión de Ventas)

Para dirigir las notificaciones de pedidos express a la persona responsable del área de ventas o cambiar el número de recepción, el sistema ofrece tres mecanismos dinámicos (sin necesidad de recompilar ni hacer redeploy):

### Opción 1 — Actualizar el número de teléfono del usuario responsable
Los usuarios con roles habilitados (default: `ROLE_ADMIN`, `ROLE_DISTRIBUTOR`) reciben notificaciones. Para asignar el celular de la persona de Ventas:
- **Vía API / Panel**: Actualizar el campo `phone` del usuario correspondiente (ej. `+5493704516054`).
- **Vía SQL**:
  ```sql
  UPDATE users SET phone = '+549XXXXXXXXXX' WHERE email = 'ventas@distribuidora.com';
  ```
El `ArgentinePhoneNormalizer` formateará automáticamente cualquier formato argentino a E.164 (`+549XXXXXXXXXX`).

### Opción 2 — Cambiar los roles receptores (`notify_roles_csv`)
Si se crea un rol específico para el encargado de ventas (ejemplo: `ROLE_SALES` o `ROLE_VENTAS`), se puede modificar la configuración global en `BusinessConfig`:
- **Vía API Admin**: `PUT /api/config/business` especificando `notifyRolesCsv = "ROLE_VENTAS"`.
- **Vía SQL**:
  ```sql
  UPDATE business_config SET notify_roles_csv = 'ROLE_VENTAS,ROLE_ADMIN';
  ```
Todos los usuarios activos con ese rol recibirán automáticamente las notificaciones.

### Opción 3 — Asignar un usuario específico por UUID (`notify_user_ids_csv`)
Si se desea notificar a una persona en particular sin importar su rol global:
- Cargar el UUID del usuario de ventas en `BusinessConfig.notify_user_ids_csv`:
  ```sql
  UPDATE business_config SET notify_user_ids_csv = 'a4d3caf6-de98-40ad-b031-cd4db6736d6c';
  ```
El resolver de destinatarios (`NotificationRecipientResolver`) incluirá al usuario deduplicando la lista.