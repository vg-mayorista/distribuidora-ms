package com.distribuidora.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.distribuidora.dto.product.ProductResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * Deserialization helper for Spring
 * {@link org.springframework.data.domain.Page}
 * responses in tests.
 *
 * <p>
 * Jackson cannot deserialize {@code Page} directly because it is an interface.
 * This record maps the JSON fields that Spring's page serialization produces
 * and reconstructs a concrete {@link PageImpl} from them.
 */
public record PageWrapper<T>(
    @JsonProperty("content") List<T> content,
    @JsonProperty("number") int number,
    @JsonProperty("size") int size,
    @JsonProperty("totalElements") long totalElements,
    @JsonProperty("totalPages") int totalPages) {
  @JsonCreator
  public PageWrapper {
    // record canonical constructor
  }

  /**
   * Convert this wrapper to a real Spring {@link PageImpl} for assertions.
   */
  public org.springframework.data.domain.Page<T> toPage() {
    return new PageImpl<>(content, PageRequest.of(number, size), totalElements);
  }
}
