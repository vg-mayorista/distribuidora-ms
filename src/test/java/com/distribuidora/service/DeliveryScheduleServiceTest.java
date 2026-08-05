package com.distribuidora.service;

import com.distribuidora.model.DeliveryWindow;
import com.distribuidora.repository.DeliveryWindowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryScheduleServiceTest {

    private static final ZoneId ARG = ZoneId.of("America/Argentina/Buenos_Aires");

    @Mock
    DeliveryWindowRepository repository;

    private final List<DeliveryWindow> windows = new ArrayList<>();
    private DeliveryScheduleService service;

    @BeforeEach
    void setUp() {
        windows.clear();
        windows.add(window(2, LocalTime.of(18, 0), 3, "Mar 18h -> Mie", true));
        windows.add(window(4, LocalTime.of(18, 0), 5, "Jue 18h -> Vie", true));
        when(repository.findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc())
                .thenAnswer(inv -> List.copyOf(windows));
        service = new DeliveryScheduleService(repository, Clock.fixed(
                Instant.parse("2026-08-04T10:00:00Z"), ZoneOffset.UTC));
        // Override the clock to use AR zone for LocalDate.now()
        // The service keeps an AtomicReference clock, so we just set it to fixed AR zone.
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-04T13:00:00-03:00"), ARG));
    }

    @Test
    void getNextDeliveryDates_returnsWedAndFriWhenBeforeCutoff() {
        // Martes 10:00 AR -> entregas Mie y Vie disponibles.
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-04T10:00:00-03:00"), ARG));
        List<LocalDate> next = service.getNextDeliveryDates(2);
        assertThat(next).hasSize(2);
        assertThat(next.get(0).getDayOfWeek().toString()).isEqualTo("WEDNESDAY");
        assertThat(next.get(1).getDayOfWeek().toString()).isEqualTo("FRIDAY");
    }

    @Test
    void getNextDeliveryDates_skipsWindowAfterCutoff() {
        // Martes 19:00 AR -> corte de Mie ya pasó; ofrece Vie y Mie de la otra semana.
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-04T19:00:00-03:00"), ARG));
        List<LocalDate> next = service.getNextDeliveryDates(2);
        assertThat(next).hasSize(2);
        // Mié (misma semana) ya no debe estar
        assertThat(next.get(0).getDayOfWeek().toString()).isEqualTo("FRIDAY");
        // Segunda fecha es Mie de la semana siguiente
        assertThat(next.get(1).getDayOfWeek().toString()).isEqualTo("WEDNESDAY");
        assertThat(next.get(1)).isAfter(next.get(0));
    }

    @Test
    void getNextDeliveryDates_todayIsNotAvailableAfterCutoff() {
        // Miercoles 10:00 AR -> mismo dia entrega, corte de Mar ya pasó.
        // El cutoff para Mie fue Mar 18:00 AR, así que ya no está disponible.
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-05T10:00:00-03:00"), ARG));
        List<LocalDate> next = service.getNextDeliveryDates(2);
        assertThat(next).hasSize(2);
        assertThat(next.get(0).getDayOfWeek().toString()).isEqualTo("FRIDAY");
        assertThat(next.get(1).getDayOfWeek().toString()).isEqualTo("WEDNESDAY");
        assertThat(next.get(1)).isAfter(next.get(0));
    }

    @Test
    void getNextDeliveryDates_lateWednesday_keepsFridayAsNext() {
        // Miercoles 19:00 AR -> mismo dia entrega, corte de Mar ya pasó.
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-05T19:00:00-03:00"), ARG));
        List<LocalDate> next = service.getNextDeliveryDates(1);
        assertThat(next).hasSize(1);
        assertThat(next.get(0).getDayOfWeek().toString()).isEqualTo("FRIDAY");
    }

    @Test
    void getNextDeliveryDates_returnsEmptyWhenNoWindows() {
        windows.clear();
        when(repository.findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc())
                .thenReturn(List.of());
        // Domingo 10:00 AR sin ventanas
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-09T10:00:00-03:00"), ARG));
        assertThat(service.getNextDeliveryDates(2)).isEmpty();
    }

    @Test
    void getCutoffFor_deliveryWednesday_returnsTuesday18AR() {
        LocalDate wed = LocalDate.parse("2026-08-05"); // miercoles
        Optional<Instant> cutoff = service.getCutoffFor(wed);
        assertThat(cutoff).isPresent();
        // Esperado: martes 2026-08-04 18:00 AR = 2026-08-04T21:00:00Z
        assertThat(cutoff.get()).isEqualTo(Instant.parse("2026-08-04T21:00:00Z"));
    }

    @Test
    void getCutoffFor_deliveryFriday_returnsThursday18AR() {
        LocalDate fri = LocalDate.parse("2026-08-07"); // viernes
        Optional<Instant> cutoff = service.getCutoffFor(fri);
        assertThat(cutoff).isPresent();
        // Esperado: jueves 2026-08-06 18:00 AR = 2026-08-06T21:00:00Z
        assertThat(cutoff.get()).isEqualTo(Instant.parse("2026-08-06T21:00:00Z"));
    }

    @Test
    void getCutoffFor_returnsEmptyWhenNoMatchingDeliveryDow() {
        LocalDate mon = LocalDate.parse("2026-08-03"); // lunes
        assertThat(service.getCutoffFor(mon)).isEmpty();
    }

    @Test
    void isWithinWindow_trueBeforeCutoff() {
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-04T17:59:00-03:00"), ARG));
        LocalDate wed = LocalDate.parse("2026-08-05");
        assertThat(service.isWithinWindow(wed)).isTrue();
    }

    @Test
    void isWithinWindow_falseAtAndAfterCutoff() {
        // inclusive cutoff: 18:00 ya no cuenta
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-04T18:00:00-03:00"), ARG));
        LocalDate wed = LocalDate.parse("2026-08-05");
        assertThat(service.isWithinWindow(wed)).isFalse();

        service.setClock(Clock.fixed(
                Instant.parse("2026-08-04T19:00:00-03:00"), ARG));
        assertThat(service.isWithinWindow(wed)).isFalse();
    }

    @Test
    void disabledWindowsAreIgnored() {
        windows.clear();
        windows.add(window(2, LocalTime.of(18, 0), 3, "Mar 18h -> Mie", false));
        when(repository.findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc())
                .thenReturn(List.of());
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-04T10:00:00-03:00"), ARG));
        assertThat(service.getNextDeliveryDates(2)).isEmpty();
        assertThat(service.getCutoffFor(LocalDate.parse("2026-08-05"))).isEmpty();
    }

    @Test
    void sameDayWindowWithEarlyCutoffIsRespected() {
        // Ventana "pedidos hasta 09:00 -> entrega mismo día a las 14:00" (no es el caso real,
        // pero valida el camino de cutoffDow == deliveryDow)
        windows.clear();
        windows.add(window(3, LocalTime.of(9, 0), 3, "Mie 9h -> Mie (mismo dia)", true));
        when(repository.findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc())
                .thenAnswer(inv -> List.copyOf(windows));

        LocalDate wed = LocalDate.parse("2026-08-05");

        // Antes del corte (08:00 AR) -> disponible
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-05T08:00:00-03:00"), ARG));
        assertThat(service.isWithinWindow(wed)).isTrue();
        assertThat(service.getNextDeliveryDates(1)).contains(wed);

        // Después del corte (10:00 AR) -> no disponible
        service.setClock(Clock.fixed(
                Instant.parse("2026-08-05T10:00:00-03:00"), ARG));
        assertThat(service.isWithinWindow(wed)).isFalse();
    }

    private DeliveryWindow window(int cutoffDow, LocalTime cutoffTime, int deliveryDow,
                                  String description, boolean active) {
        return DeliveryWindow.builder()
                .cutoffDayOfWeek(cutoffDow)
                .cutoffTime(cutoffTime)
                .deliveryDayOfWeek(deliveryDow)
                .description(description)
                .active(active)
                .build();
    }
}
