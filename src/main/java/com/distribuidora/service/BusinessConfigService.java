package com.distribuidora.service;

import com.distribuidora.dto.config.BusinessConfigResponse;
import com.distribuidora.dto.config.UpdateBusinessConfigRequest;
import com.distribuidora.model.BusinessConfig;
import com.distribuidora.repository.BusinessConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessConfigService {

    public static final BigDecimal DEFAULT_MIN_ORDER_AMOUNT = new BigDecimal("30000.00");
    public static final int DEFAULT_MIN_ORDER_UNITS = 5;

    private final BusinessConfigRepository businessConfigRepository;

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
        return new BusinessConfigResponse(
                config.getMinOrderAmount(),
                config.getMinOrderUnits(),
                config.getUpdatedAt()
        );
    }

    public BusinessConfigResponse updateConfig(UpdateBusinessConfigRequest req) {
        BusinessConfig config = businessConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> BusinessConfig.builder().build());

        config.setMinOrderAmount(req.minOrderAmount());
        config.setMinOrderUnits(req.minOrderUnits());
        config.setUpdatedAt(Instant.now());

        BusinessConfig saved = businessConfigRepository.save(config);
        return new BusinessConfigResponse(
                saved.getMinOrderAmount(),
                saved.getMinOrderUnits(),
                saved.getUpdatedAt()
        );
    }
}
