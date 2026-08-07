package com.distribuidora.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email es inválido")
    @Size(max = 255, message = "El email no puede superar los 255 caracteres")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
    String password,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String firstName,

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    String lastName,

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(
        regexp = "^[0-9 -]{8,20}$",
        message = "El teléfono debe tener un formato argentino válido (solo números, espacios y guiones)"
    )
    String phone,

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 500, message = "La dirección no puede superar los 500 caracteres")
    String address,

    @Size(max = 100, message = "La zona no puede superar los 100 caracteres")
    String zone,

    String latitude,

    String longitude,

    @Size(max = 50, message = "El rol no puede superar los 50 caracteres")
    String role
) {}
