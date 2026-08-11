package com.distribuidora.service;

import com.distribuidora.dto.delivery.CreateDeliveryMethodRequest;
import com.distribuidora.dto.delivery.PatchDeliveryMethodRequest;
import com.distribuidora.dto.delivery.UpdateDeliveryMethodRequest;
import com.distribuidora.exception.DeliveryMethodNotFoundException;
import com.distribuidora.exception.DuplicateDeliveryMethodException;
import com.distribuidora.mapper.DeliveryMethodMapper;
import com.distribuidora.model.DeliveryMethod;
import com.distribuidora.model.DeliveryMethodScope;
import com.distribuidora.repository.DeliveryMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryMethodServiceTest {

    @Mock
    DeliveryMethodRepository repository;

    @Mock
    DeliveryMethodMapper mapper;

    @InjectMocks
    DeliveryMethodService service;

    @Captor
    ArgumentCaptor<DeliveryMethod> deliveryMethodCaptor;

    private DeliveryMethod dm;
    private UUID dmId;

    @BeforeEach
    void setUp() {
        dmId = UUID.randomUUID();
        dm = DeliveryMethod.builder()
            .id(dmId)
            .name("Envío Express")
            .cost(new BigDecimal("150.00"))
            .estimatedDays(3)
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    // --- create ---

    @Test
    void create_ok() {
        var req = new CreateDeliveryMethodRequest("Envío Express",
            new BigDecimal("150.00"), 3, DeliveryMethodScope.BOTH);
        when(repository.existsByName("Envío Express")).thenReturn(false);
        when(mapper.toEntity(req)).thenReturn(dm);
        when(repository.save(any(DeliveryMethod.class))).thenReturn(dm);

        var result = service.create(req);

        assertThat(result).isSameAs(dm);
        verify(repository).save(any(DeliveryMethod.class));
    }

    @Test
    void create_duplicateName_throws() {
        var req = new CreateDeliveryMethodRequest("Envío Express",
            new BigDecimal("150.00"), 3, DeliveryMethodScope.BOTH);
        when(repository.existsByName("Envío Express")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(DuplicateDeliveryMethodException.class)
            .hasMessageContaining("Envío Express");
        verify(repository, never()).save(any());
    }

    // --- getById ---

    @Test
    void getById_active_ok() {
        when(repository.findByIdAndActiveTrue(dmId)).thenReturn(Optional.of(dm));

        var result = service.getById(dmId);

        assertThat(result).isSameAs(dm);
    }

    @Test
    void getById_notFound_throws() {
        when(repository.findByIdAndActiveTrue(dmId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(dmId))
            .isInstanceOf(DeliveryMethodNotFoundException.class);
    }

    // --- list ---

    @Test
    void list_activeTrue_returnsActivePage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DeliveryMethod> page = new PageImpl<>(List.of(dm));
        when(repository.findByActiveTrue(pageable)).thenReturn(page);

        var result = service.list(pageable, true);

        assertThat(result.getContent()).containsExactly(dm);
    }

    @Test
    void list_activeNull_returnsActivePage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DeliveryMethod> page = new PageImpl<>(List.of(dm));
        when(repository.findByActiveTrue(pageable)).thenReturn(page);

        var result = service.list(pageable, null);

        assertThat(result.getContent()).containsExactly(dm);
    }

    @Test
    void list_activeFalse_returnsInactivePage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DeliveryMethod> page = new PageImpl<>(List.of(dm));
        when(repository.findByActiveFalse(pageable)).thenReturn(page);

        var result = service.list(pageable, false);

        assertThat(result.getContent()).containsExactly(dm);
    }

    // --- update ---

    @Test
    void update_ok() {
        var req = new UpdateDeliveryMethodRequest("Envío Premium",
            new BigDecimal("200.00"), 5, DeliveryMethodScope.BOTH);
        when(repository.findByIdAndActiveTrue(dmId)).thenReturn(Optional.of(dm));
        when(repository.existsByNameAndIdNot("Envío Premium", dmId)).thenReturn(false);
        // applyUpdate is void; make the mock actually mutate the entity
        doAnswer(inv -> {
            DeliveryMethod target = inv.getArgument(0);
            UpdateDeliveryMethodRequest r = inv.getArgument(1);
            target.setName(r.name());
            target.setCost(r.cost());
            target.setEstimatedDays(r.estimatedDays());
            return null;
        }).when(mapper).applyUpdate(any(), any());

        var result = service.update(dmId, req);

        assertThat(result).isSameAs(dm);
        assertThat(dm.getName()).isEqualTo("Envío Premium");
        assertThat(dm.getCost()).isEqualByComparingTo("200.00");
    }

    @Test
    void update_notFound_throws() {
        var req = new UpdateDeliveryMethodRequest("Envío Premium",
            new BigDecimal("200.00"), 5, DeliveryMethodScope.BOTH);
        when(repository.findByIdAndActiveTrue(dmId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(dmId, req))
            .isInstanceOf(DeliveryMethodNotFoundException.class);
    }

    @Test
    void update_duplicateName_throws() {
        var req = new UpdateDeliveryMethodRequest("Envío Express",
            new BigDecimal("200.00"), 5, DeliveryMethodScope.BOTH);
        when(repository.findByIdAndActiveTrue(dmId)).thenReturn(Optional.of(dm));
        when(repository.existsByNameAndIdNot("Envío Express", dmId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(dmId, req))
            .isInstanceOf(DuplicateDeliveryMethodException.class);
    }

    // --- patch ---

    @Test
    void patch_partial_ok() {
        var req = new PatchDeliveryMethodRequest(null, new BigDecimal("180.00"), null, null);
        when(repository.findByIdAndActiveTrue(dmId)).thenReturn(Optional.of(dm));
        // applyPatch is void; make the mock actually mutate the entity
        doAnswer(inv -> {
            DeliveryMethod target = inv.getArgument(0);
            PatchDeliveryMethodRequest r = inv.getArgument(1);
            if (r.name() != null) target.setName(r.name());
            if (r.cost() != null) target.setCost(r.cost());
            if (r.estimatedDays() != null) target.setEstimatedDays(r.estimatedDays());
            return null;
        }).when(mapper).applyPatch(any(), any());

        var result = service.patch(dmId, req);

        assertThat(result).isSameAs(dm);
        assertThat(dm.getCost()).isEqualByComparingTo("180.00");
    }

    @Test
    void patch_nameChange_validatesUniqueness() {
        var req = new PatchDeliveryMethodRequest("Nuevo Nombre", null, null, null);
        when(repository.findByIdAndActiveTrue(dmId)).thenReturn(Optional.of(dm));
        when(repository.existsByNameAndIdNot("Nuevo Nombre", dmId)).thenReturn(true);

        assertThatThrownBy(() -> service.patch(dmId, req))
            .isInstanceOf(DuplicateDeliveryMethodException.class);
    }

    // --- softDelete ---

    @Test
    void softDelete_active_setsInactive() {
        when(repository.findById(dmId)).thenReturn(Optional.of(dm));

        service.softDelete(dmId);

        assertThat(dm.getActive()).isFalse();
    }

    @Test
    void softDelete_alreadyInactive_idempotent() {
        dm.setActive(false);
        when(repository.findById(dmId)).thenReturn(Optional.of(dm));

        service.softDelete(dmId);

        assertThat(dm.getActive()).isFalse(); // still false
    }

    @Test
    void softDelete_notFound_throws() {
        when(repository.findById(dmId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(dmId))
            .isInstanceOf(DeliveryMethodNotFoundException.class);
    }

    // --- activate ---

    @Test
    void activate_inactive_setsActive() {
        dm.setActive(false);
        when(repository.findById(dmId)).thenReturn(Optional.of(dm));

        var result = service.activate(dmId);

        assertThat(result.getActive()).isTrue();
    }

    @Test
    void activate_alreadyActive_idempotent() {
        when(repository.findById(dmId)).thenReturn(Optional.of(dm));

        var result = service.activate(dmId);

        assertThat(result.getActive()).isTrue();
    }

    @Test
    void activate_notFound_throws() {
        when(repository.findById(dmId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(dmId))
            .isInstanceOf(DeliveryMethodNotFoundException.class);
    }
}
