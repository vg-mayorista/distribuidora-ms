package com.distribuidora.repository;
import com.distribuidora.model.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Product}.
 *
 * <p>
 * Methods that target soft-deleted rows are explicit (prefixed with
 * {@code Active});
 * default lookups ({@link #findById(Object)}) may return soft-deleted entities
 * on
 * purpose, so the service layer can decide what to do (e.g. raise 404 on a
 * public
 * read, reactivate on a PATCH).
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

  Page<Product> findByActiveTrue(Pageable pageable);

  Optional<Product> findByIdAndActiveTrue(UUID id);

  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, UUID id);

  boolean existsByCategoryIdAndActiveTrue(UUID categoryId);

  boolean existsByIdAndActiveTrue(UUID id);

  Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);
}
