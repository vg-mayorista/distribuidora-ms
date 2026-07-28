package com.distribuidora.mapper;
import com.distribuidora.model.Category;

import com.distribuidora.dto.category.CreateCategoryRequest;
import com.distribuidora.dto.category.UpdateCategoryRequest;
import org.springframework.stereotype.Component;

/**
 * Plain (non-MapStruct) mapper between {@link Category} and its DTOs.
 *
 * <p>
 * Stays a Spring component so it can be evolved with custom logic without
 * churning the call sites.
 */
@Component
public class CategoryMapper {

    /**
     * Build a new entity from a create request.
     *
     * <p>
     * Defaults applied here: {@code active = true}. Timestamps are set by
     * JPA lifecycle callbacks, not the mapper.
     */
    public Category toEntity(CreateCategoryRequest req) {
        return Category.builder()
                .name(req.name())
                .active(Boolean.TRUE)
                .build();
    }

    /**
     * Apply a full replacement update to an existing entity.
     *
     * <p>
     * {@code active}, {@code createdAt} and {@code updatedAt} are intentionally
     * not touched &mdash; they are owned by the service/lifecycle.
     */
    public void applyUpdate(Category target, UpdateCategoryRequest req) {
        target.setName(req.name());
    }

}
