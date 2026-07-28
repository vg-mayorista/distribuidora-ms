package com.distribuidora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

/**
 * Global CORS configuration for the application.
 *
 * <p>Exposes a CorsConfigurationSource (not a CorsFilter) so Spring Security
 * can integrate it directly into its filter chain. This ensures CORS headers
 * are added even to rejected requests (401, 403, etc.), fixing the bug where
 * unauthenticated requests returned bare responses without CORS headers.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origins allowed to make cross-origin requests
        // ⚠️ IMPORTANTE: setAllowedOrigins() NO soporta wildcards
        // Usamos setAllowedOriginPatterns() que sí soporta patrones con *
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",           // Cualquier puerto localhost
            "http://127.0.0.1:*",           // Cualquier puerto 127.0.0.1
            "https://*.vercel.app",          // Todos los subdominios de Vercel
            "https://*.serveo.net",          // Todos los subdominios serveo.net
            "https://*.serveousercontent.com", // Todos los serveousercontent.com
            "https://*.trycloudflare.com",   // Cloudflare Tunnel
            "https://*.ngrok-free.app",      // Ngrok Free
            "https://*.ngrok.io",            // Ngrok.io
            "https://*.ngrok-free.app/",      // Ngrok Free (app subdomain)
            "https://*.ngrok-free.dev/"      // Ngrok Free (dev subdomain)
        ));

        // HTTP methods allowed
        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Headers allowed in requests
        // NOTE: Cannot use "*" with allowCredentials(true) — Spring rejects it at runtime.
        config.setAllowedHeaders(List.of(
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // How long the browser caches the preflight response
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
