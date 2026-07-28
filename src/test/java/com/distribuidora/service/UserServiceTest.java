package com.distribuidora.service;

import com.distribuidora.dto.user.CustomerSummaryResponse;
import com.distribuidora.model.Role;
import com.distribuidora.model.User;
import com.distribuidora.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private User sampleCustomer(boolean active) {
        Role customerRole = Role.builder()
                .id(UUID.randomUUID())
                .name("ROLE_CUSTOMER")
                .description("Customer role")
                .build();

        return User.builder()
                .id(CUSTOMER_ID)
                .email("customer@test.com")
                .firstName("John")
                .lastName("Doe")
                .active(active)
                .role(customerRole)
                .createdAt(Instant.now())
                .build();
    }

    private User sampleAdmin() {
        Role adminRole = Role.builder()
                .id(UUID.randomUUID())
                .name("ROLE_ADMIN")
                .description("Admin role")
                .build();

        return User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("User")
                .active(true)
                .role(adminRole)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    class GetCustomers {

        @Test
        void returnsCustomersSuccessfully() {
            Pageable pageable = PageRequest.of(0, 10);
            User customer = sampleCustomer(true);
            Page<User> page = new PageImpl<>(List.of(customer), pageable, 1);

            when(userRepository.findByRoleNameAndSearch(null, "John", pageable)).thenReturn(page);

            Page<CustomerSummaryResponse> result = userService.getCustomers("John", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).email()).isEqualTo("customer@test.com");
            assertThat(result.getContent().get(0).active()).isTrue();
        }
    }

    @Nested
    class ToggleCustomerActive {

        @Test
        void togglesActiveStateFromTrueToFalse() {
            User activeCustomer = sampleCustomer(true);
            when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(activeCustomer));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CustomerSummaryResponse result = userService.toggleCustomerActive(CUSTOMER_ID);

            assertThat(result.active()).isFalse();
            verify(userRepository).save(activeCustomer);
        }

        @Test
        void togglesActiveStateFromFalseToTrue() {
            User inactiveCustomer = sampleCustomer(false);
            when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(inactiveCustomer));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CustomerSummaryResponse result = userService.toggleCustomerActive(CUSTOMER_ID);

            assertThat(result.active()).isTrue();
            verify(userRepository).save(inactiveCustomer);
        }

        @Test
        void throwsExceptionWhenUserNotFound() {
            when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.toggleCustomerActive(CUSTOMER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cliente no encontrado");

            verify(userRepository, never()).save(any(User.class));
        }
    }
}
