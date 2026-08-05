package com.distribuidora.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Configuración global del negocio (singleton). Reglas que aplica hoy:
 *
 * <ul>
 *   <li>{@link #getMinPacksPerLine()} — mínimo de packs por línea en cada
 *       pedido (mayorista o stock).</li>
 *   <li>{@link #getMinOrderAmount()} — monto mínimo del subtotal del pedido
 *       (mayorista o stock).</li>
 * </ul>
 */
@Entity
@Table(name = "business_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_packs_per_line", nullable = false)
    @Builder.Default
    private Integer minPacksPerLine = 5;

    @Column(name = "min_order_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = new BigDecimal("30000.00");

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (this.minPacksPerLine == null) this.minPacksPerLine = 5;
        if (this.minOrderAmount == null) this.minOrderAmount = new BigDecimal("30000.00");
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
