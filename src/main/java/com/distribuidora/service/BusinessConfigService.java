package com.distribuidora.service;

import com.distribuidora.dto.config.BusinessConfigResponse;
import com.distribuidora.dto.config.UpdateBusinessConfigRequest;
import com.distribuidora.dto.delivery.DeliveryWindowResponse;
import com.distribuidora.model.BusinessConfig;
import com.distribuidora.repository.BusinessConfigRepository;
import com.distribuidora.repository.DeliveryWindowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessConfigService {

    public static final BigDecimal DEFAULT_MIN_ORDER_AMOUNT = new BigDecimal("30000.00");
    public static final int DEFAULT_MIN_ORDER_UNITS = 5;

    private final BusinessConfigRepository businessConfigRepository;
    private final DeliveryWindowRepository deliveryWindowRepository;

    @Transactional(readOnly = true)
    public BusinessConfig getOrInitConfig() {
        return businessConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> BusinessConfig.builder()
                        .minOrderAmount(DEFAULT_MIN_ORDER_AMOUNT)
                        .minOrderUnits(DEFAULT_MIN_ORDER_UNITS)
                        .updatedAt(Instant.now())
                        .build());
    }

    @Transactional(readOnly = true)
    public BusinessConfigResponse getPublicConfig() {
        BusinessConfig config = getOrInitConfig();
        return toResponse(config);
    }

    public BusinessConfigResponse updateConfig(UpdateBusinessConfigRequest req) {
        BusinessConfig config = businessConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> BusinessConfig.builder().build());

        config.setMinOrderAmount(req.minOrderAmount());
        config.setMinOrderUnits(req.minOrderUnits());
        config.setUpdatedAt(Instant.now());

        BusinessConfig saved = businessConfigRepository.save(config);
        return toResponse(saved);
    }

    private BusinessConfigResponse toResponse(BusinessConfig config) {
        List<DeliveryWindowResponse> windows = deliveryWindowRepository
                .findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc()
                .stream()
                .map(DeliveryWindowResponse::from)
                .toList();
        return new BusinessConfigResponse(
                config.getMinOrderAmount(),
                config.getMinOrderUnits(),
                windows,
                config.getUpdatedAt()
        );
    }
}
