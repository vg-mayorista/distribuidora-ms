package com.distribuidora.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Configurable weekly delivery window. Each window defines:
 * <ul>
 *   <li>{@code cutoffDayOfWeek} + {@code cutoffTime}: cierre del pedido.</li>
 *   <li>{@code deliveryDayOfWeek}: día de la semana en que se reparte.</li>
 * </ul>
 * Seed inicial: martes 18:00 → miércoles, jueves 18:00 → viernes.
 *
 * <p>Day of week uses ISO numbering (1 = lunes … 7 = domingo).
 */
@Entity
@Table(name = "delivery_windows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @Min(1)
    @Max(7)
    @Column(name = "cutoff_day_of_week", nullable = false)
    private Integer cutoffDayOfWeek;

    @NotNull
    @Column(name = "cutoff_time", nullable = false)
    private LocalTime cutoffTime;

    @NotNull
    @Min(1)
    @Max(7)
    @Column(name = "delivery_day_of_week", nullable = false)
    private Integer deliveryDayOfWeek;

    @Size(max = 100)
    @Column(length = 100)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
