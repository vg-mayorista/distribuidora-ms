package com.distribuidora.dto.user;

import com.distribuidora.model.User;
import java.time.Instant;
import java.util.UUID;

public record CustomerSummaryResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String role,
    Boolean active,
    Instant createdAt
) {
    public static CustomerSummaryResponse from(User user) {
        return new CustomerSummaryResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole().getName(),
            user.getActive(),
            user.getCreatedAt()
        );
    }
}
