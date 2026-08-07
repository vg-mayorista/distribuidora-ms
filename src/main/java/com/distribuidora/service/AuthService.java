package com.distribuidora.service;

import com.distribuidora.model.User;
import com.distribuidora.model.Role;
import com.distribuidora.repository.UserRepository;
import com.distribuidora.repository.RoleRepository;
import com.distribuidora.config.security.CustomUserDetails;
import com.distribuidora.config.security.JwtService;
import com.distribuidora.dto.user.AuthResponse;
import com.distribuidora.dto.user.LoginRequest;
import com.distribuidora.dto.user.RegisterRequest;
import com.distribuidora.dto.user.LoginResult;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.distribuidora.config.security.InMemoryTokenBlacklistService;
import java.math.BigDecimal;
import java.util.Date;

@Service
public class AuthService {

  private static final java.util.Set<String> VALID_ROLES =
      java.util.Set.of("ROLE_CUSTOMER", "ROLE_DISTRIBUTOR", "ROLE_ADMIN");

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final InMemoryTokenBlacklistService tokenBlacklistService;

  public AuthService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuthenticationManager authenticationManager,
      UserDetailsService userDetailsService,
      InMemoryTokenBlacklistService tokenBlacklistService) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.tokenBlacklistService = tokenBlacklistService;
  }

  @Transactional
  public com.distribuidora.dto.user.RegisterResult register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      return new com.distribuidora.dto.user.RegisterResult.DuplicateEmail(request.email());
    }

    String roleName = request.role() == null || request.role().isBlank()
        ? "ROLE_CUSTOMER"
        : request.role().toUpperCase();

    if (!VALID_ROLES.contains(roleName)) {
      throw new IllegalArgumentException(
          "Rol inválido: '" + roleName + "'. Debe ser uno de: " + VALID_ROLES);
    }

    Role userRole = roleRepository.findByName(roleName)
        .orElseGet(() -> roleRepository.save(
            Role.builder()
                .name(roleName)
                .description("Auto-created role: " + roleName)
                .build()));

    User user = User.builder()
        .email(request.email())
        .password(passwordEncoder.encode(request.password()))
        .firstName(request.firstName())
        .lastName(request.lastName())
        .phone(request.phone())
        .address(request.address())
        .zone(request.zone())
        .latitude(parseCoord(request.latitude()))
        .longitude(parseCoord(request.longitude()))
        .role(userRole)
        .active(true)
        .build();

    User savedUser = userRepository.save(user);
    CustomUserDetails userDetails = new CustomUserDetails(savedUser);
    String token = jwtService.generateToken(userDetails);

    AuthResponse response = toAuthResponse(token, savedUser);

    return new com.distribuidora.dto.user.RegisterResult.Success(response);
  }

  @Transactional(readOnly = true)
  public LoginResult login(LoginRequest request) {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.email(), request.password()));

      CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(request.email());
      User user = userDetails.getUser();
      String token = jwtService.generateToken(userDetails);

      AuthResponse response = toAuthResponse(token, user);
      return new LoginResult.Success(response);
    } catch (org.springframework.security.core.AuthenticationException e) {
      return new LoginResult.InvalidCredentials();
    }
  }

  public void logout(String token) {
    Date expiration = jwtService.extractExpiration(token);
    tokenBlacklistService.blacklistToken(token, expiration);
  }

  private static AuthResponse toAuthResponse(String token, User user) {
    String lat = user.getLatitude() == null ? null : user.getLatitude().toPlainString();
    String lng = user.getLongitude() == null ? null : user.getLongitude().toPlainString();
    return new AuthResponse(
        token,
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getRole().getName(),
        user.getAddress(),
        user.getPhone(),
        user.getZone(),
        lat,
        lng);
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
