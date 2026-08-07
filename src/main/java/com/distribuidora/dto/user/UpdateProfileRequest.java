package com.distribuidora.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 100, message = "Nombre máximo 100 caracteres")
    String firstName,

    @Size(max = 100, message = "Apellido máximo 100 caracteres")
    String lastName,

    @Size(max = 500, message = "Dirección máximo 500 caracteres")
    String address,

    @Pattern(
        regexp = "^[0-9 -]{8,20}$",
        message = "El teléfono debe tener un formato argentino válido (solo números, espacios y guiones)"
    )
    String phone,

    @Size(max = 100, message = "Zona máximo 100 caracteres")
    String zone,

    String latitude,

    String longitude
) {}
