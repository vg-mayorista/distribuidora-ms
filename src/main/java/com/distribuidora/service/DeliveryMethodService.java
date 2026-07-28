package com.distribuidora.service;

import com.distribuidora.dto.delivery.CreateDeliveryMethodRequest;
import com.distribuidora.dto.delivery.PatchDeliveryMethodRequest;
import com.distribuidora.dto.delivery.UpdateDeliveryMethodRequest;
import com.distribuidora.exception.DeliveryMethodNotFoundException;
import com.distribuidora.exception.DuplicateDeliveryMethodException;
import com.distribuidora.mapper.DeliveryMethodMapper;
import com.distribuidora.model.DeliveryMethod;
import com.distribuidora.repository.DeliveryMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business layer for the {@link DeliveryMethod} entity.
 *
 * <p>Public reads only see {@code active = true} methods.
 * Administrative operations (deactivate / reactivate) ignore that flag.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryMethodService {

    private final DeliveryMethodRepository repository;
    private final DeliveryMethodMapper mapper;

    /**
     * Create a new delivery method.
     *
     * @throws DuplicateDeliveryMethodException if name already exists
     */
    public DeliveryMethod create(CreateDeliveryMethodRequest req) {
        if (repository.existsByName(req.name())) {
            throw new DuplicateDeliveryMethodException(req.name());
        }
        DeliveryMethod dm = mapper.toEntity(req);
        return repository.save(dm);
    }

    /**
     * Get a delivery method by ID (active only).
     *
     * @throws DeliveryMethodNotFoundException if not found or inactive
     */
    @Transactional(readOnly = true)
    public DeliveryMethod getById(UUID id) {
        return repository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new DeliveryMethodNotFoundException(id));
    }

    /**
     * List delivery methods with optional active filter.
     */
    @Transactional(readOnly = true)
    public Page<DeliveryMethod> list(Pageable pageable, Boolean active) {
        if (active == null || active) {
            return repository.findByActiveTrue(pageable);
        }
        return repository.findByActiveFalse(pageable);
    }

    /**
     * Full update of a delivery method.
     *
     * @throws DeliveryMethodNotFoundException if not found or inactive
     * @throws DuplicateDeliveryMethodException if new name already exists
     */
    public DeliveryMethod update(UUID id, UpdateDeliveryMethodRequest req) {
        DeliveryMethod dm = repository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new DeliveryMethodNotFoundException(id));
        if (repository.existsByNameAndIdNot(req.name(), id)) {
            throw new DuplicateDeliveryMethodException(req.name());
        }
        mapper.applyUpdate(dm, req);
        return dm;
    }

    /**
     * Partial update of a delivery method.
     *
     * <p>Only non-null fields from the request are applied.
     *
     * @throws DeliveryMethodNotFoundException if not found or inactive
     * @throws DuplicateDeliveryMethodException if new name already exists
     */
    public DeliveryMethod patch(UUID id, PatchDeliveryMethodRequest req) {
        DeliveryMethod dm = repository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new DeliveryMethodNotFoundException(id));

        // Check name uniqueness if name is being changed
        if (req.name() != null && !req.name().equals(dm.getName())) {
            if (repository.existsByNameAndIdNot(req.name(), id)) {
                throw new DuplicateDeliveryMethodException(req.name());
            }
        }

        mapper.applyPatch(dm, req);
        return dm;
    }

    /**
     * Soft delete (deactivate) a delivery method.
     *
     * <p>Idempotent: returns silently if already inactive.
     *
     * @throws DeliveryMethodNotFoundException if ID does not exist
     */
    public void softDelete(UUID id) {
        DeliveryMethod dm = repository.findById(id)
            .orElseThrow(() -> new DeliveryMethodNotFoundException(id));
        if (Boolean.FALSE.equals(dm.getActive())) {
            return; // already inactive
        }
        dm.setActive(Boolean.FALSE);
    }

    /**
     * Reactivate a previously deactivated delivery method.
     *
     * <p>Idempotent: returns the entity even if already active.
     *
     * @throws DeliveryMethodNotFoundException if ID does not exist
     */
    public DeliveryMethod activate(UUID id) {
        DeliveryMethod dm = repository.findById(id)
            .orElseThrow(() -> new DeliveryMethodNotFoundException(id));
        dm.setActive(Boolean.TRUE);
        return dm;
    }
}
