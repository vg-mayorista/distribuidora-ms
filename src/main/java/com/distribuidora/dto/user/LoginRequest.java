package com.distribuidora.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email es inválido")
    @Size(max = 255, message = "El email no puede superar los 255 caracteres")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres")
    String password
) {}
