package com.distribuidora.config.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final InMemoryTokenBlacklistService tokenBlacklistService;

  public JwtAuthenticationFilter(
      JwtService jwtService,
      UserDetailsService userDetailsService,
      InMemoryTokenBlacklistService tokenBlacklistService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
    this.tokenBlacklistService = tokenBlacklistService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {
    final String authHeader = request.getHeader("Authorization");
    final String jwt;
    final String userEmail;

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    jwt = authHeader.substring(7);
    if (tokenBlacklistService.isBlacklisted(jwt)) {
      writeProblemDetailResponse(response, "El token JWT ha sido invalidado (sesión cerrada)",
          HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    try {
      userEmail = jwtService.extractUsername(jwt);
    } catch (ExpiredJwtException e) {
      writeProblemDetailResponse(response, "El token JWT ha expirado", HttpServletResponse.SC_UNAUTHORIZED);
      return;
    } catch (SignatureException | MalformedJwtException e) {
      writeProblemDetailResponse(response, "El token JWT es inválido o la firma no coincide",
          HttpServletResponse.SC_UNAUTHORIZED);
      return;
    } catch (Exception e) {
      writeProblemDetailResponse(response, "Error al procesar el token JWT", HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
        if (jwtService.isTokenValid(jwt, userDetails)) {
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
              userDetails,
              null,
              userDetails.getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      } catch (Exception e) {
        // user not found or other load error
      }
    }
    filterChain.doFilter(request, response);
  }

  private void writeProblemDetailResponse(HttpServletResponse response, String message, int status) throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json;charset=UTF-8");
    String json = String.format(
        "{\"title\":\"Unauthorized\",\"status\":%d,\"detail\":\"%s\",\"instance\":\"\"}",
        status, message);
    response.getWriter().write(json);
  }
}
