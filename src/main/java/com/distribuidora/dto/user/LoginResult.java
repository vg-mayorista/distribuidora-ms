package com.distribuidora.dto.user;

public sealed interface LoginResult {
    record Success(AuthResponse response) implements LoginResult {}
    record InvalidCredentials() implements LoginResult {}
}
