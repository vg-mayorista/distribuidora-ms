package com.distribuidora.dto.user;

import com.distribuidora.model.User;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String address,
    String phone
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getAddress(),
            user.getPhone()
        );
    }
}
