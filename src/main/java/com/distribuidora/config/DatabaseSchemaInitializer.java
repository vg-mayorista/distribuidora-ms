package com.distribuidora.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Asegurando tipo TEXT para la columna payment_receipt_url en la tabla orders...");
            jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN payment_receipt_url TYPE TEXT");
            log.info("Columna payment_receipt_url migrada a TEXT exitosamente.");
        } catch (Exception e) {
            log.warn("Resultado al intentar migrar payment_receipt_url a TEXT: {}", e.getMessage());
        }
    }
}
