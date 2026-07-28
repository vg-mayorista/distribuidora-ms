package com.distribuidora.repository;

import com.distribuidora.model.DeliveryMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DeliveryMethod}.
 */
@Repository
public interface DeliveryMethodRepository extends JpaRepository<DeliveryMethod, UUID> {

    // Active-only queries (public reads)

    Page<DeliveryMethod> findByActiveTrue(Pageable pageable);

    Optional<DeliveryMethod> findByIdAndActiveTrue(UUID id);

    boolean existsByIdAndActiveTrue(UUID id);

    // Inactive queries (admin reads)

    Page<DeliveryMethod> findByActiveFalse(Pageable pageable);

    // Uniqueness checks

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
