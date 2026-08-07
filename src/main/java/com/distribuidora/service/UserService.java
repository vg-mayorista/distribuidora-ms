package com.distribuidora.service;

import com.distribuidora.dto.user.CustomerSummaryResponse;
import com.distribuidora.dto.user.UpdateProfileRequest;
import com.distribuidora.dto.user.UserProfileResponse;
import com.distribuidora.model.User;
import com.distribuidora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.zone() != null) {
            user.setZone(request.zone());
        }
        if (request.latitude() != null) {
            user.setLatitude(parseCoord(request.latitude()));
        }
        if (request.longitude() != null) {
            user.setLongitude(parseCoord(request.longitude()));
        }

        User saved = userRepository.save(user);
        return UserProfileResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> getCustomers(String search, Pageable pageable) {
        return userRepository.findByRoleNameAndSearch(null, search, pageable)
                .map(CustomerSummaryResponse::from);
    }

    @Transactional
    public CustomerSummaryResponse toggleCustomerActive(UUID customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if ("ROLE_ADMIN".equals(user.getRole().getName())) {
            throw new RuntimeException("No se puede desactivar a un usuario administrador.");
        }

        user.setActive(!user.getActive());
        User saved = userRepository.save(user);
        return CustomerSummaryResponse.from(saved);
    }

    private static BigDecimal parseCoord(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

