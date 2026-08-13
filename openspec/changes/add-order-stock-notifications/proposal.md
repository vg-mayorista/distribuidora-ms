# Cambios: Notificaciones internas de pedidos STOCK (express)

## Why

Cuando entra un pedido `OrderType.STOCK` (entrega inmediata contra excedente en
depósito), el dueño / distribuidores hoy se enteran **solo si están mirando el
panel**. La distribuidora quiere reaccionar rápido ante un express: armar el
pedido en el momento, coordinar envío, llamar al cliente si hay un problema.

Hoy no hay ningún canal de aviso. Tampoco hay forma de saber si algo falló en un
envío posterior (no existe ni siquiera el concepto de "notificación enviada").

## What Changes

- **Disparador**: al persistir un pedido `OrderType.STOCK` (`createStock`),
  se publica un `ApplicationEvent` que se procesa **post-commit** y de forma
  **asíncrona** (no bloquea el `201 Created` al cliente).
- **Destinatarios**: usuarios internos activos con rol en
  `BusinessConfig.notifyRolesCsv` (default `ROLE_ADMIN,ROLE_DISTRIBUTOR`),
  más una lista opcional `notifyUserIdsCsv` para sumar ids puntuales. **Nunca
  se notifica al cliente**.
- **Canales MVP**:
  - **WhatsApp** vía Twilio (utility template → cuando aprobemos el template
    propio en Meta Business Manager, ~24 h).
  - **Email** vía SMTP (Spring Mail). Dev: MailHog. Prod: Resend / SendGrid /
    SES.
- **Skip por canal**: si un destinatario no tiene phone válido, el WhatsApp
  para ese destinatario queda como `SKIPPED` y se sigue con el email (y
  viceversa). Un destinatario sin datos no bloquea al resto.
- **Persistencia**: nueva tabla `notification_log` con un row por
  `(recipient × channel × intento)`. Permite auditoría, retry manual y
  estadísticas.
- **Resiliencia**: `@Async` con `ThreadPoolTaskExecutor` dedicado + `@Retryable`
  con backoff exponencial (3 intentos, 2s → 4s → 8s).
- **Feature flags** en `BusinessConfig` para apagar canales / triggers sin
  redeploy.
- **Endpoints admin** (`ROLE_ADMIN`):
  - `GET  /api/admin/notifications` — log paginado con filtros.
  - `GET  /api/admin/notifications/{id}` — detalle.
  - `POST /api/admin/notifications/{id}/retry` — reintento manual.
  - `GET  /api/admin/notifications/stats` — contadores por canal/status.

## Impact

- **DB**: 1 CREATE TABLE (`notification_log`) + 5 ALTER COLUMN en
  `business_config`. `ddl-auto: update` lo aplica solo en el próximo arranque.
- **API**: nuevos DTOs (`NotificationLogResponse`); nuevos endpoints admin.
- **Email SMTP**: dependencia nueva (`spring-boot-starter-mail`).
- **Twilio SDK**: dependencia nueva (`com.twilio.sdk:twilio 10.7.0`).
- **Spring Retry**: dependencias nuevas (`spring-retry` + `spring-aspects`).
- **Env vars nuevas**:
  ```
  TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_FROM
  SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD
  MAIL_FROM_ADDRESS, MAIL_FROM_NAME
  NOTIFY_WHATSAPP_ENABLED, NOTIFY_EMAIL_ENABLED
  NOTIFY_RETRY_MAX_ATTEMPTS, NOTIFY_RETRY_INITIAL_BACKOFF_MS
  ```
- **Riesgos**: el template de Meta requiere aprobación de Meta (~24 h). En
  sandbox de Twilio solo podemos mandar a números que mandaron `join` (ok
  para dev, no para prod).

## Estado de la sesión 2026-08-13

- ✅ Cuenta Twilio conectada y validada en desarrollo y producción (Render).
- ✅ Notificaciones por WhatsApp mediante Twilio API **100% Verificadas y Operativas**.
- ✅ Historial, métricas y reintento manual implementados y probados.
- ✅ Guía para modificar número de recepción y roles de ventas integrada en la especificación SDD (`openspec/specs/notifications/spec.md`).

Plan operativo completo en `.plans/order-stock-notifications.md` (gitignored).