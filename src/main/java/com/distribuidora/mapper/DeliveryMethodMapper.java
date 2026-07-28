package com.distribuidora.mapper;

import com.distribuidora.dto.delivery.CreateDeliveryMethodRequest;
import com.distribuidora.dto.delivery.PatchDeliveryMethodRequest;
import com.distribuidora.dto.delivery.UpdateDeliveryMethodRequest;
import com.distribuidora.model.DeliveryMethod;
import org.springframework.stereotype.Component;

/**
 * Plain (non-MapStruct) mapper between {@link DeliveryMethod} and its DTOs.
 */
@Component
public class DeliveryMethodMapper {

    /**
     * Build a new entity from a create request.
     *
     * <p>Defaults applied here: {@code active = true}, {@code estimatedDays = 0}.
     * Timestamps are set by JPA lifecycle callbacks.
     */
    public DeliveryMethod toEntity(CreateDeliveryMethodRequest req) {
        return DeliveryMethod.builder()
            .name(req.name())
            .cost(req.cost())
            .estimatedDays(req.estimatedDays())
            .active(Boolean.TRUE)
            .build();
    }

    /**
     * Apply a full replacement update to an existing entity.
     *
     * <p>{@code active}, {@code createdAt} and {@code updatedAt} are intentionally
     * not touched — they are owned by the service/lifecycle.
     */
    public void applyUpdate(DeliveryMethod target, UpdateDeliveryMethodRequest req) {
        target.setName(req.name());
        target.setCost(req.cost());
        target.setEstimatedDays(req.estimatedDays());
    }

    /**
     * Apply a partial update to an existing entity.
     *
     * <p>Only non-null fields from the request are applied.
     */
    public void applyPatch(DeliveryMethod target, PatchDeliveryMethodRequest req) {
        if (req.name() != null) {
            target.setName(req.name());
        }
        if (req.cost() != null) {
            target.setCost(req.cost());
        }
        if (req.estimatedDays() != null) {
            target.setEstimatedDays(req.estimatedDays());
        }
    }
}
