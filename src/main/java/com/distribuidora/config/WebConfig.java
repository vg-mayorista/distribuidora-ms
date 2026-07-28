package com.distribuidora.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the API test harness from the {@code projects/} directory so it runs
 * on the same origin as the API — no CORS issues, no extra HTTP server.
 *
 * <p>Access: <a href="http://localhost:8080/test-harness.html">http://localhost:8080/test-harness.html</a>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/test-harness.html")
                .addResourceLocations("file:projects/");
    }
}
