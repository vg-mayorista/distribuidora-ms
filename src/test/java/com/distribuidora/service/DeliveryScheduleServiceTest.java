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
        service = newServiceAt(Instant.parse("2026-08-04T13:00:00-03:00"));
    }

    private DeliveryScheduleService newServiceAt(Instant instant) {
        return new DeliveryScheduleService(repository, Clock.fixed(instant, ARG));
    }

    @Test
    void getNextDeliveryDates_returnsWedAndFriWhenBeforeCutoff() {
        // Martes 10:00 AR -> entregas Mie y Vie disponibles.
        service = newServiceAt(Instant.parse("2026-08-04T10:00:00-03:00"));
        List<LocalDate> next = service.getNextDeliveryDates(2);
        assertThat(next).hasSize(2);
        assertThat(next.get(0).getDayOfWeek().toString()).isEqualTo("WEDNESDAY");
        assertThat(next.get(1).getDayOfWeek().toString()).isEqualTo("FRIDAY");
    }

    @Test
    void getNextDeliveryDates_skipsWindowAfterCutoff() {
        // Martes 19:00 AR -> corte de Mie ya pasó; ofrece Vie y Mie de la otra semana.
        service = newServiceAt(Instant.parse("2026-08-04T19:00:00-03:00"));
        List<LocalDate> next = service.getNextDeliveryDates(2);
        assertThat(next).hasSize(2);
        assertThat(next.get(0).getDayOfWeek().toString()).isEqualTo("FRIDAY");
        assertThat(next.get(1).getDayOfWeek().toString()).isEqualTo("WEDNESDAY");
        assertThat(next.get(1)).isAfter(next.get(0));
    }

    @Test
    void getNextDeliveryDates_todayIsNotAvailableAfterCutoff() {
        // Miercoles 10:00 AR -> mismo dia entrega, corte de Mar ya pasó.
        service = newServiceAt(Instant.parse("2026-08-05T10:00:00-03:00"));
        List<LocalDate> next = service.getNextDeliveryDates(2);
        assertThat(next).hasSize(2);
        assertThat(next.get(0).getDayOfWeek().toString()).isEqualTo("FRIDAY");
        assertThat(next.get(1).getDayOfWeek().toString()).isEqualTo("WEDNESDAY");
    }

    @Test
    void getNextDeliveryDates_lateWednesday_keepsFridayAsNext() {
        service = newServiceAt(Instant.parse("2026-08-05T19:00:00-03:00"));
        List<LocalDate> next = service.getNextDeliveryDates(1);
        assertThat(next).hasSize(1);
        assertThat(next.get(0).getDayOfWeek().toString()).isEqualTo("FRIDAY");
    }

    @Test
    void getNextDeliveryDates_returnsEmptyWhenNoWindows() {
        windows.clear();
        when(repository.findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc())
                .thenReturn(List.of());
        service = newServiceAt(Instant.parse("2026-08-09T10:00:00-03:00"));
        assertThat(service.getNextDeliveryDates(2)).isEmpty();
    }

    @Test
    void getCutoffFor_deliveryWednesday_returnsTuesday18AR() {
        LocalDate wed = LocalDate.parse("2026-08-05");
        Optional<Instant> cutoff = service.getCutoffFor(wed);
        assertThat(cutoff).isPresent();
        assertThat(cutoff.get()).isEqualTo(Instant.parse("2026-08-04T21:00:00Z"));
    }

    @Test
    void getCutoffFor_deliveryFriday_returnsThursday18AR() {
        LocalDate fri = LocalDate.parse("2026-08-07");
        Optional<Instant> cutoff = service.getCutoffFor(fri);
        assertThat(cutoff).isPresent();
        assertThat(cutoff.get()).isEqualTo(Instant.parse("2026-08-06T21:00:00Z"));
    }

    @Test
    void getCutoffFor_returnsEmptyWhenNoMatchingDeliveryDow() {
        LocalDate mon = LocalDate.parse("2026-08-03");
        assertThat(service.getCutoffFor(mon)).isEmpty();
    }

    @Test
    void isWithinWindow_trueBeforeCutoff() {
        service = newServiceAt(Instant.parse("2026-08-04T17:59:00-03:00"));
        LocalDate wed = LocalDate.parse("2026-08-05");
        assertThat(service.isWithinWindow(wed)).isTrue();
    }

    @Test
    void isWithinWindow_falseAtAndAfterCutoff() {
        LocalDate wed = LocalDate.parse("2026-08-05");
        service = newServiceAt(Instant.parse("2026-08-04T18:00:00-03:00"));
        assertThat(service.isWithinWindow(wed)).isFalse();

        service = newServiceAt(Instant.parse("2026-08-04T19:00:00-03:00"));
        assertThat(service.isWithinWindow(wed)).isFalse();
    }

    @Test
    void disabledWindowsAreIgnored() {
        windows.clear();
        windows.add(window(2, LocalTime.of(18, 0), 3, "Mar 18h -> Mie", false));
        when(repository.findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc())
                .thenReturn(List.of());
        service = newServiceAt(Instant.parse("2026-08-04T10:00:00-03:00"));
        assertThat(service.getNextDeliveryDates(2)).isEmpty();
        assertThat(service.getCutoffFor(LocalDate.parse("2026-08-05"))).isEmpty();
    }

    @Test
    void sameDayWindowWithEarlyCutoffIsRespected() {
        windows.clear();
        windows.add(window(3, LocalTime.of(9, 0), 3, "Mie 9h -> Mie (mismo dia)", true));
        when(repository.findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc())
                .thenAnswer(inv -> List.copyOf(windows));

        LocalDate wed = LocalDate.parse("2026-08-05");

        service = newServiceAt(Instant.parse("2026-08-05T08:00:00-03:00"));
        assertThat(service.isWithinWindow(wed)).isTrue();
        assertThat(service.getNextDeliveryDates(1)).contains(wed);

        service = newServiceAt(Instant.parse("2026-08-05T10:00:00-03:00"));
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
