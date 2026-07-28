package com.distribuidora.dto.user;

public sealed interface RegisterResult {
    record Success(AuthResponse response) implements RegisterResult {}
    record DuplicateEmail(String email) implements RegisterResult {}
}
