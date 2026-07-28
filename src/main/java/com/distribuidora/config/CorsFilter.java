package com.distribuidora.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Fallback CORS filter that runs before Spring Security.
 * This ensures CORS headers are set on ALL responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        String origin = request.getHeader("Origin");
        
        // Allow localhost and serveo/ngrok domains
        if (origin == null || origin.contains("localhost")
            || origin.contains("serveo.net") || origin.contains("serveousercontent.com")
            || origin.contains("ngrok-free.app") || origin.contains("ngrok-free.dev")
            || origin.contains("ngrok.io") || origin.contains("trycloudflare.com")) {

            if (origin == null) {
                // Wildcard origin is incompatible with credentials
                response.setHeader("Access-Control-Allow-Origin", "*");
            } else {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }
            
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", 
                "Content-Type, Authorization, X-Requested-With, Accept, Origin");
            response.setHeader("Access-Control-Max-Age", "3600");
        }

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }
}
