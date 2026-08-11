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

- [ ] `pom.xml`: agregar `spring-boot-starter-mail`,
      `com.twilio.sdk:twilio 10.7.0`, `spring-retry`, `spring-aspects`.
- [ ] `application.yml`: bloque `spring.mail.*` + `notify.*`.
- [ ] `notification/config/NotificationProperties.java`
      (`@ConfigurationProperties("notify")`).
- [ ] `notification/config/AsyncConfig.java` (`@EnableAsync` +
      `ThreadPoolTaskExecutor`).
- [ ] `notification/config/RetryConfig.java` (`@EnableRetry`).

## Phase N2 — Modelo de datos

- [ ] `model/BusinessConfig.java`: +5 columnas
      (`notifyRolesCsv`, `notifyUserIdsCsv`, `notifyWhatsappEnabled`,
      `notifyEmailEnabled`, `notifyOnStockOrderCreated`) + helper
      `getNotifyRoles()`.
- [ ] `notification/domain/NotificationChannel.java` (enum).
- [ ] `notification/domain/NotificationTrigger.java` (enum).
- [ ] `notification/domain/NotificationStatus.java` (enum).
- [ ] `notification/domain/NotificationLog.java` (entidad JPA).
- [ ] `notification/repository/NotificationLogRepository.java`.
- [ ] `repository/UserRepository.java`: query
      `findActiveByRoleNames(Collection<String>)`.
- [ ] `seeder/DatabaseSeeder.java`: defaults en `true` para los flags.

## Phase N3 — Canal Email (primera iteración funcional)

- [ ] `notification/service/channel/NotificationChannelSender.java` (interface).
- [ ] `notification/service/channel/EmailSender.java` (Spring Mail).
- [ ] `notification/service/template/OrderNotificationRenderer.java`
      (genera subject + body HTML + texto plano).
- [ ] `resources/messages.properties` (templates parametrizados).
- [ ] Levantar MailHog local (`docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog`).
- [ ] Test end-to-end: crear pedido STOCK → verificar que llega mail al
      admin / distribuidor y queda row en `notification_log`.

## Phase N4 — Disparador desacoplado

- [ ] `notification/service/event/OrderNotificationEvent.java`
      (ApplicationEvent).
- [ ] `notification/service/event/OrderNotificationListener.java`
      (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`).
- [ ] `service/OrderService.java`: inyectar `ApplicationEventPublisher`,
      publicar evento post-`save` en `createStock`.

## Phase N5 — Resolución de destinatarios

- [ ] `notification/recipient/NotificationRecipientResolver.java`:
      une users por roles + ids extra, deduplica, filtra `active = true`.

## Phase N6 — Canal WhatsApp

- [ ] `notification/service/channel/WhatsAppSender.java` (Twilio SDK).
- [ ] Normalizador de teléfonos ARG (`+549XXXXXXXXXX`).
- [ ] Crear template utility `vg_pedido_express_recibido` en Meta Business
      Manager y obtener `Template SID` (~24 h aprobación).
- [ ] Cargar SID en `messages.properties`.
- [ ] Test end-to-end: crear pedido STOCK → WhatsApp llega al admin /
      distribuidor.

## Phase N7 — Orquestación y resiliencia

- [ ] `notification/service/NotificationService.java` (entry point).
- [ ] `notification/service/NotificationDispatcher.java` (fan-out
      recipient × channel).
- [ ] `@Retryable` en cada `send()` con
      `maxAttempts = ${notify.retry.max-attempts}` y
      `backoff = 2s × multiplier 2.0`.
- [ ] `@Async("notificationExecutor")` en el listener.

## Phase N8 — Endpoints admin

- [ ] `notification/controller/AdminNotificationController.java`.
- [ ] `notification/dto/NotificationLogResponse.java`.
- [ ] `notification/exception/NotificationDeliveryException.java`.
- [ ] Filtros por `status`, `orderId`, `channel`. Paginación.

## Phase N9 — Tests

- [ ] Unit: `OrderNotificationListenerTest` (mock dispatcher, verifica que
      no dispara para `WHOLESALE`).
- [ ] Unit: `WhatsAppSender.normalizeArPhone` con casos
      (`1145678900`, `+5491156789000`, `15-4567-8900`, `null`).
- [ ] Unit: `NotificationRecipientResolver` (dedup roles + ids extra).
- [ ] Integration (H2): crear pedido STOCK → 2 rows en `notification_log`
      (1 email + 1 WA por destinatario).
- [ ] Integration: `POST /api/admin/notifications/{id}/retry`.

## Backlog (no implementado en este PR)

- Upgrade de cuenta Twilio a producción + sender propio aprobado por Meta.
- Sumar otros triggers: `ORDER_STOCK_STATUS_CHANGED` (ARMADO/ENVIADO/ENTREGADO),
  `ORDER_CANCELLED`.
- Tabla `notification_recipients` con opt-in/opt-out por canal por user.
- Job de purga de `notification_log` (PII) a 90 días.
- Soporte para SMS / Telegram / push notifications.
- Dashboard de stats en frontend (admin).