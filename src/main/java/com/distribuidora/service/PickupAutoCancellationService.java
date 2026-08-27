package com.distribuidora.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PickupAutoCancellationService {

    private final OrderService orderService;

    /**
     * Tarea programada que se ejecuta cada hora para desintegrar/cancelar
     * automáticamente los pedidos de retiro en local que hayan superado las 24 hs.
     */
    @Scheduled(cron = "0 5 * * * *")
    public void runAutoCancellation() {
        log.info("Ejecutando proceso programado de desintegración de pedidos de retiro vencidos (24hs)...");
        try {
            int count = orderService.cancelAllExpiredPickupOrders();
            if (count > 0) {
                log.info("Se desintegraron {} pedidos de retiro vencidos automáticamente.", count);
            }
        } catch (Exception e) {
            log.error("Error al desintegrar pedidos de retiro vencidos: {}", e.getMessage(), e);
        }
    }
}
