package com.distribuidora.service;

import com.distribuidora.dto.delivery.CreateDeliveryWindowRequest;
import com.distribuidora.dto.delivery.DeliveryWindowResponse;
import com.distribuidora.dto.delivery.UpdateDeliveryWindowRequest;
import com.distribuidora.exception.DeliveryWindowNotFoundException;
import com.distribuidora.model.DeliveryWindow;
import com.distribuidora.repository.DeliveryWindowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryWindowService {

    private final DeliveryWindowRepository repository;

    @Transactional(readOnly = true)
    public List<DeliveryWindowResponse> listActive() {
        return repository.findByActiveTrueOrderByCutoffDayOfWeekAscCutoffTimeAsc()
                .stream()
                .map(DeliveryWindowResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryWindowResponse> listAll() {
        return repository.findAllByOrderByCutoffDayOfWeekAscCutoffTimeAsc()
                .stream()
                .map(DeliveryWindowResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliveryWindowResponse get(UUID id) {
        return DeliveryWindowResponse.from(loadOrThrow(id));
    }

    public DeliveryWindowResponse create(CreateDeliveryWindowRequest req) {
        DeliveryWindow window = DeliveryWindow.builder()
                .cutoffDayOfWeek(req.cutoffDayOfWeek())
                .cutoffTime(req.cutoffTime())
                .deliveryDayOfWeek(req.deliveryDayOfWeek())
                .description(req.description())
                .active(req.active() == null ? Boolean.TRUE : req.active())
                .updatedAt(Instant.now())
                .build();
        return DeliveryWindowResponse.from(repository.save(window));
    }

    public DeliveryWindowResponse update(UUID id, UpdateDeliveryWindowRequest req) {
        DeliveryWindow window = loadOrThrow(id);
        if (req.cutoffDayOfWeek() != null) window.setCutoffDayOfWeek(req.cutoffDayOfWeek());
        if (req.cutoffTime() != null) window.setCutoffTime(req.cutoffTime());
        if (req.deliveryDayOfWeek() != null) window.setDeliveryDayOfWeek(req.deliveryDayOfWeek());
        if (req.description() != null) window.setDescription(req.description());
        if (req.active() != null) window.setActive(req.active());
        window.setUpdatedAt(Instant.now());
        return DeliveryWindowResponse.from(window);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private DeliveryWindow loadOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DeliveryWindowNotFoundException(id));
    }
}
