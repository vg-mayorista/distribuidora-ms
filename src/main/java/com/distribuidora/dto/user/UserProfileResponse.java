package com.distribuidora.dto.user;

import com.distribuidora.model.User;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String address,
    String phone,
    String zone,
    String latitude,
    String longitude
) {
    public static UserProfileResponse from(User user) {
        String lat = user.getLatitude() == null ? null : user.getLatitude().toPlainString();
        String lng = user.getLongitude() == null ? null : user.getLongitude().toPlainString();
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getAddress(),
            user.getPhone(),
            user.getZone(),
            lat,
            lng
        );
    }
}
