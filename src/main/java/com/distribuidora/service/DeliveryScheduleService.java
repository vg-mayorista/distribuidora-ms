package com.distribuidora.service;

import com.distribuidora.model.DeliveryWindow;
import com.distribuidora.repository.DeliveryWindowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
@Transactional(readOnly = true)
public class DeliveryScheduleService {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    public static final int MAX_LOOKAHEAD_DAYS = 14;

    private final DeliveryWindowRepository deliveryWindowRepository;
    private final Clock clock;
    private final ZoneId zoneId = DEFAULT_ZONE;

    public DeliveryScheduleService(DeliveryWindowRepository deliveryWindowRepository) {
        this(deliveryWindowRepository, Clock.system(DEFAULT_ZONE));
    }

    /** Constructor para tests o config beans: permite inyectar un {@link Clock} fijo. */
    public DeliveryScheduleService(DeliveryWindowRepository deliveryWindowRepository, Clock clock) {
        this.deliveryWindowRepository = deliveryWindowRepository;
        this.clock = clock;
    }

    public Clock clock() {
        return clock;
    }

    /**
     * Devuelve hasta {@code n} fechas de entrega futuras (incluyendo hoy si todavía está
     * disponible) cuyo cutoff aún no haya pasado. Orden ascendente.
     */
    public List<LocalDate> getNextDeliveryDates(int n) {
        if (n <= 0) return List.of();
        LocalDate today = LocalDate.now(clock.withZone(zoneId));
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

    public Optional<Instant> getCutoffFor(LocalDate deliveryDate) {
        int deliveryDow = deliveryDate.getDayOfWeek().getValue();
        return activeWindows().stream()
                .filter(w -> w.getDeliveryDayOfWeek() != null
                        && w.getDeliveryDayOfWeek() == deliveryDow)
                .map(w -> toCutoffInstant(deliveryDate, w))
                .min(Comparator.naturalOrder());
    }

    public boolean isWithinWindow(LocalDate deliveryDate) {
        Instant now = clock.instant();
        return getCutoffFor(deliveryDate).map(now::isBefore).orElse(false);
    }

    public boolean isWithinWindowAt(LocalDate deliveryDate, Instant now) {
        return getCutoffFor(deliveryDate).map(now::isBefore).orElse(false);
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private List<DeliveryWindow> activeWindows() {
        return deliveryWindowRepository.findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc();
    }

    private boolean isAvailableFor(LocalDate candidate, List<DeliveryWindow> windows) {
        Instant now = clock.instant();
        return getCutoffFor(candidate).filter(instant -> instant.isAfter(now)).isPresent();
    }

    private Instant toCutoffInstant(LocalDate deliveryDate, DeliveryWindow w) {
        LocalTime cutoffTime = w.getCutoffTime() != null ? w.getCutoffTime() : LocalTime.MIDNIGHT;
        int deliveryDow = deliveryDate.getDayOfWeek().getValue();
        int cutoffDow = w.getCutoffDayOfWeek() != null ? w.getCutoffDayOfWeek() : deliveryDow;
        long daysBetween = Math.floorMod(deliveryDow - cutoffDow, 7);
        LocalDate cutoffDate = deliveryDate.minusDays(daysBetween);
        LocalDateTime cutoffLdt = LocalDateTime.of(cutoffDate, cutoffTime);
        return cutoffLdt.atZone(zoneId).toInstant();
    }
}
