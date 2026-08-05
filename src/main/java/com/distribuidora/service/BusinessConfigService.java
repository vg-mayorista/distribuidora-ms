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

    public static final int DEFAULT_MIN_PACKS_PER_LINE = 5;
    public static final BigDecimal DEFAULT_MIN_ORDER_AMOUNT = new BigDecimal("30000.00");

    private final BusinessConfigRepository businessConfigRepository;
    private final DeliveryWindowRepository deliveryWindowRepository;

    @Transactional(readOnly = true)
    public BusinessConfig getOrInitConfig() {
        return businessConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> BusinessConfig.builder()
                        .minPacksPerLine(DEFAULT_MIN_PACKS_PER_LINE)
                        .minOrderAmount(DEFAULT_MIN_ORDER_AMOUNT)
                        .updatedAt(Instant.now())
                        .build());
    }

    @Transactional(readOnly = true)
    public BusinessConfigResponse getPublicConfig() {
        return toResponse(getOrInitConfig());
    }

    public BusinessConfigResponse updateConfig(UpdateBusinessConfigRequest req) {
        BusinessConfig config = getOrInitConfig();
        config.setMinPacksPerLine(req.minPacksPerLine());
        config.setMinOrderAmount(req.minOrderAmount());
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
                config.getMinPacksPerLine(),
                config.getMinOrderAmount(),
                windows,
                config.getUpdatedAt()
        );
    }
}
