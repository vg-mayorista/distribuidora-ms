package com.distribuidora.dto.user;

import java.util.UUID;

public record AuthResponse(
    String token,
    UUID id,
    String email,
    String firstName,
    String lastName,
    String role,
    String address,
    String phone
) {}
