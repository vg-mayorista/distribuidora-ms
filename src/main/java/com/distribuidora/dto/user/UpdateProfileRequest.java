package com.distribuidora.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 100, message = "Nombre máximo 100 caracteres")
    String firstName,

    @Size(max = 100, message = "Apellido máximo 100 caracteres")
    String lastName,

    @Size(max = 500, message = "Dirección máximo 500 caracteres")
    String address,

    @Size(max = 20, message = "Teléfono máximo 20 caracteres")
    String phone
) {}
