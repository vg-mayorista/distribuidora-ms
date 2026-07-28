package com.distribuidora.repository;
import com.distribuidora.model.Category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Category}.
 *
 * <p>Methods that target soft-deleted rows are explicit (prefixed with
 * {@code Active}); default lookups may return soft-deleted entities on purpose,
 * so the service layer can decide what to do.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Active-only queries (public reads)

    Page<Category> findByActiveTrue(Pageable pageable);

    Optional<Category> findByIdAndActiveTrue(UUID id);

    boolean existsByIdAndActiveTrue(UUID id);

    // Inactive queries (admin reads)

    Page<Category> findByActiveFalse(Pageable pageable);

    // Uniqueness checks

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
