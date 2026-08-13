# Tasks: Notificaciones internas de pedidos STOCK (express)

Status legend: `[x]` done · `[~]` in progress · `[ ]` pending

## Phase N0 — Configuración externa (✅)

- [x] Cuenta Twilio creada y validada por API.
- [x] Auth Token rotado: primary dado de baja, en uso el **secondary**.
- [x] WhatsApp Sandbox conectado (`whatsapp:+14155238886`).
- [x] Mensaje de prueba enviado y recibido desde el sandbox.
- [x] `.env` local creado (gitignored) con `TWILIO_*` + `SPRING_MAIL_*` +
      `NOTIFY_*`.
- [x] `env.example` documentado con placeholders.

## Phase N1 — Dependencias y configuración base

- [x] `pom.xml`: agregar `spring-boot-starter-mail`,
      `com.twilio.sdk:twilio 10.7.0`, `spring-retry`, `spring-aspects`.
- [x] `application.yml`: bloque `spring.mail.*` + `notify.*`.
- [x] `notification/config/NotificationProperties.java`
      (`@ConfigurationProperties("notify")`).
- [x] `notification/config/AsyncConfig.java` (`@EnableAsync` +
      `ThreadPoolTaskExecutor`).
- [x] `notification/config/RetryConfig.java` (`@EnableRetry`).

## Phase N2 — Modelo de datos

- [x] `model/BusinessConfig.java`: +5 columnas
      (`notifyRolesCsv`, `notifyUserIdsCsv`, `notifyWhatsappEnabled`,
      `notifyEmailEnabled`, `notifyOnStockOrderCreated`) + helper
      `getNotifyRoles()`.
- [x] `notification/domain/NotificationChannel.java` (enum).
- [x] `notification/domain/NotificationTrigger.java` (enum).
- [x] `notification/domain/NotificationStatus.java` (enum).
- [x] `notification/domain/NotificationLog.java` (entidad JPA).
- [x] `notification/repository/NotificationLogRepository.java`.
- [x] `repository/UserRepository.java`: query
      `findActiveByRoleNames(Collection<String>)`.
- [x] `seeder/DatabaseSeeder.java`: defaults en `true` para los flags.

## Phase N3 — Canal Email (primera iteración funcional)

- [x] `notification/service/channel/NotificationChannelSender.java` (interface).
- [x] `notification/service/channel/EmailSender.java` (Spring Mail).
- [x] `notification/service/template/OrderNotificationRenderer.java`
      (genera subject + body HTML + texto plano).
- [x] `resources/messages.properties` (templates parametrizados).
- [x] Levantar MailHog local (`docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog`).
- [x] Test end-to-end: crear pedido STOCK → verificar que llega mail al
      admin / distribuidor y queda row en `notification_log`.

## Phase N4 — Disparador desacoplado

- [x] `notification/service/event/OrderNotificationEvent.java`
      (ApplicationEvent).
- [x] `notification/service/event/OrderNotificationListener.java`
      (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`).
- [x] `service/OrderService.java`: inyectar `ApplicationEventPublisher`,
      publicar evento post-`save` en `createStock`.

## Phase N5 — Resolución de destinatarios

- [x] `notification/recipient/NotificationRecipientResolver.java`:
      une users por roles + ids extra, deduplica, filtra `active = true`.

## Phase N6 — Canal WhatsApp

- [x] `notification/service/channel/WhatsAppSender.java` (Twilio SDK).
- [x] Normalizador de teléfonos ARG (`+549XXXXXXXXXX`).
- [x] Crear template utility `vg_pedido_express_recibido` en Meta Business
      Manager y obtener `Template SID` (~24 h aprobación).
- [x] Cargar SID en `messages.properties`.
- [x] Test end-to-end: crear pedido STOCK → WhatsApp llega al admin /
      distribuidor.

## Phase N7 — Orquestación y resiliencia

- [x] `notification/service/NotificationService.java` (entry point).
- [x] `notification/service/NotificationDispatcher.java` (fan-out
      recipient × channel).
- [x] `@Retryable` en cada `send()` con
      `maxAttempts = ${notify.retry.max-attempts}` y
      `backoff = 2s × multiplier 2.0`.
- [x] `@Async("notificationExecutor")` en el listener.

## Phase N8 — Endpoints admin

- [x] `notification/controller/AdminNotificationController.java`.
- [x] `notification/dto/NotificationLogResponse.java`.
- [x] `notification/exception/NotificationDeliveryException.java`.
- [x] Filtros por `status`, `orderId`, `channel`. Paginación.

## Phase N9 — Tests

- [x] Unit: `OrderNotificationListenerTest` (mock dispatcher, verifica que
      no dispara para `WHOLESALE`).
- [x] Unit: `WhatsAppSender.normalizeArPhone` con casos
      (`1145678900`, `+5491156789000`, `15-4567-8900`, `null`).
- [x] Unit: `NotificationRecipientResolver` (dedup roles + ids extra).
- [x] Integration (H2): crear pedido STOCK → 2 rows en `notification_log`
      (1 email + 1 WA por destinatario).
- [x] Integration: `POST /api/admin/notifications/{id}/retry`.

## Backlog (no implementado en este PR)

- Upgrade de cuenta Twilio a producción + sender propio aprobado por Meta.
- Sumar otros triggers: `ORDER_STOCK_STATUS_CHANGED` (ARMADO/ENVIADO/ENTREGADO),
  `ORDER_CANCELLED`.
- Tabla `notification_recipients` con opt-in/opt-out por canal por user.
- Job de purga de `notification_log` (PII) a 90 días.
- Soporte para SMS / Telegram / push notifications.
- Dashboard de stats en frontend (admin).