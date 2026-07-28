package com.distribuidora.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(name = "uk_product_name", columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid")
  private UUID id;

  @NotBlank
  @Size(max = 100)
  @Column(nullable = false, length = 100)
  private String name;

  @Size(max = 500)
  @Column(length = 500)
  private String description;

  @NotNull
  @DecimalMin(value = "0.00", inclusive = true)
  @Digits(integer = 7, fraction = 2)
  @Column(nullable = false, precision = 9, scale = 2)
  private BigDecimal price;

  @Column(name = "category_id")
  private UUID categoryId;

  @Size(max = 500)
  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @NotNull
  @Min(0)
  @Column(nullable = false)
  @Builder.Default
  private Integer stock = 0;

  @NotNull
  @Min(1)
  @Column(name = "units_per_pack", nullable = false)
  @Builder.Default
  private Integer unitsPerPack = 1;

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
