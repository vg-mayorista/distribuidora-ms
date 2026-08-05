package com.distribuidora.dto.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateBusinessConfigRequest(
        @NotNull(message = "El mínimo de packs por línea no puede ser nulo")
        @Min(value = 1, message = "El mínimo de packs debe ser al menos 1")
        Integer minPacksPerLine
) {}
