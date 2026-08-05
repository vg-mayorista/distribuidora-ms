package com.distribuidora.service;

import com.distribuidora.model.DeliveryWindow;
import com.distribuidora.repository.DeliveryWindowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Servicio puro para resolver fechas de entrega según las ventanas semanales configuradas.
 *
 * <p>La invariante es que una {@code deliveryDate} es elegible para confirmar un pedido
 * mayorista si y solo si:
 * <pre>
 *   getCutoffFor(deliveryDate).isPresent() && Instant.now() < getCutoffFor(deliveryDate)
 * </pre>
 *
 * <p>Todos los cálculos se hacen en la zona horaria de la distribuidora
 * ({@code America/Argentina/Buenos_Aires}) para evitar líos con UTC.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryScheduleService {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    /**
     * Máximo de días hacia adelante que se iteran para buscar las próximas N fechas. Alcanza
     * para 2 semanas de cobertura con dos ventanas semanales.
     */
    public static final int MAX_LOOKAHEAD_DAYS = 14;

    private final DeliveryWindowRepository deliveryWindowRepository;
    private final AtomicReference<Clock> clockRef = new AtomicReference<>(Clock.system(DEFAULT_ZONE));
    private final ZoneId zoneId = DEFAULT_ZONE;

    /** Constructor para tests: permite inyectar un {@link Clock} fijo. */
    public DeliveryScheduleService(DeliveryWindowRepository deliveryWindowRepository, Clock clock) {
        this.deliveryWindowRepository = deliveryWindowRepository;
        this.clockRef.set(clock);
    }

    public Clock clock() {
        return clockRef.get();
    }

    /** Para tests. */
    public void setClock(Clock clock) {
        this.clockRef.set(clock);
    }

    /**
     * Devuelve hasta {@code n} fechas de entrega futuras (incluyendo hoy si todavía está
     * disponible) cuyo cutoff aún no haya pasado. Orden ascendente.
     */
    public List<LocalDate> getNextDeliveryDates(int n) {
        if (n <= 0) return List.of();
        LocalDate today = LocalDate.now(clockRef.get().withZone(zoneId));
        List<DeliveryWindow> windows = activeWindows();
        List<LocalDate> result = new ArrayList<>();
        for (int offset = 0; offset < MAX_LOOKAHEAD_DAYS && result.size() < n; offset++) {
            LocalDate candidate = today.plusDays(offset);
            if (isAvailableFor(candidate, windows)) {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Devuelve el {@link Instant} (UTC) del cutoff para la fecha de entrega indicada,
     * o {@link Optional#empty()} si no hay ninguna ventana activa que entregue ese día.
     */
    public Optional<Instant> getCutoffFor(LocalDate deliveryDate) {
        int deliveryDow = deliveryDate.getDayOfWeek().getValue();
        return activeWindows().stream()
                .filter(w -> w.getDeliveryDayOfWeek() != null
                        && w.getDeliveryDayOfWeek() == deliveryDow)
                .map(w -> toCutoffInstant(deliveryDate, w))
                .min(Comparator.naturalOrder());
    }

    /**
     * True si existe una ventana activa que entregue en {@code deliveryDate} y el cutoff
     * todavía no pasó.
     */
    public boolean isWithinWindow(LocalDate deliveryDate) {
        Instant now = clockRef.get().instant();
        return getCutoffFor(deliveryDate).map(now::isBefore).orElse(false);
    }

    /**
     * Variante para tests: permite pasar un {@code now} arbitrario.
     */
    public boolean isWithinWindowAt(LocalDate deliveryDate, Instant now) {
        return getCutoffFor(deliveryDate).map(now::isBefore).orElse(false);
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private List<DeliveryWindow> activeWindows() {
        return deliveryWindowRepository.findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc();
    }

    private boolean isAvailableFor(LocalDate candidate, List<DeliveryWindow> windows) {
        return getCutoffFor(candidate).filter(instant ->
                instant.isAfter(clockRef.get().instant())
        ).isPresent();
    }

    private Instant toCutoffInstant(LocalDate deliveryDate, DeliveryWindow w) {
        LocalTime cutoffTime = w.getCutoffTime() != null ? w.getCutoffTime() : LocalTime.MIDNIGHT;
        int deliveryDow = deliveryDate.getDayOfWeek().getValue();
        int cutoffDow = w.getCutoffDayOfWeek() != null ? w.getCutoffDayOfWeek() : deliveryDow;
        // días entre el cutoff y la entrega (siempre >= 0, módulo 7)
        long daysBetween = Math.floorMod(deliveryDow - cutoffDow, 7);
        if (daysBetween == 0 && w.getDeliveryDayOfWeek() != null
                && w.getCutoffDayOfWeek() != null
                && w.getDeliveryDayOfWeek().equals(w.getCutoffDayOfWeek())) {
            // mismo día: si la hora del cutoff ya pasó pero la entrega es hoy, igual lo contamos
            daysBetween = 0;
        }
        LocalDate cutoffDate = deliveryDate.minusDays(daysBetween);
        LocalDateTime cutoffLdt = LocalDateTime.of(cutoffDate, cutoffTime);
        return cutoffLdt.atZone(zoneId).toInstant();
    }

    /** Diferencia absoluta en días entre dos DayOfWeek (1..7 → ISO). */
    @SuppressWarnings("unused")
    private static long daysBetween(int fromDow, int toDow) {
        return Math.floorMod(toDow - fromDow, 7);
    }

    /** Para debug/tests: cuántos días faltan entre hoy y la próxima entrega. */
    @SuppressWarnings("unused")
    public Long daysUntilNextDelivery() {
        LocalDate next = getNextDeliveryDates(1).stream().findFirst().orElse(null);
        if (next == null) return null;
        LocalDate today = LocalDate.now(clockRef.get().withZone(zoneId));
        return ChronoUnit.DAYS.between(today, next);
    }
}
