package com.distribuidora.controller;

import com.distribuidora.service.AuthService;

import com.distribuidora.dto.user.LoginRequest;
import com.distribuidora.dto.user.RegisterRequest;
import com.distribuidora.dto.user.RegisterResult;
import com.distribuidora.dto.user.LoginResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Controlador para el registro de usuarios e inicio de sesión con JWT")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @Operation(summary = "Registrar un usuario", description = "Crea un nuevo usuario en la base de datos con el rol ROLE_CUSTOMER por defecto.")
  @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente y autenticado (retorna token)")
  @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
  @ApiResponse(responseCode = "409", description = "El correo electrónico ya está registrado")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
    RegisterResult result = authService.register(req);
    return switch (result) {
      case RegisterResult.Success success -> ResponseEntity.ok(success.response());
      case RegisterResult.DuplicateEmail duplicate -> {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detail.setDetail("El correo electrónico ya está registrado: '" + duplicate.email() + "'");
        yield ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
      }
    };
  }

  @PostMapping("/login")
  @Operation(summary = "Iniciar sesión", description = "Autentica las credenciales de un usuario y devuelve un token JWT válido.")
  @ApiResponse(responseCode = "200", description = "Autenticación exitosa (retorna token)")
  @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
    LoginResult result = authService.login(req);
    return switch (result) {
      case LoginResult.Success success -> ResponseEntity.ok(success.response());
      case LoginResult.InvalidCredentials invalid -> {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        detail.setDetail("Credenciales incorrectas");
        yield ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(detail);
      }
    };
  }

  @PostMapping("/logout")
  @Operation(summary = "Cerrar sesión", description = "Invalida el token JWT actual de forma inmediata añadiéndolo a la lista negra.")
  @ApiResponse(responseCode = "204", description = "Sesión cerrada exitosamente")
  @ApiResponse(responseCode = "401", description = "No autorizado o token no válido")
  public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      authService.logout(token);
    }
    return ResponseEntity.noContent().build();
  }
}
