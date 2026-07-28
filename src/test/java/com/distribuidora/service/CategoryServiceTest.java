package com.distribuidora.service;
import com.distribuidora.model.Category;
import com.distribuidora.repository.CategoryRepository;
import com.distribuidora.service.CategoryService;
import com.distribuidora.mapper.CategoryMapper;

import com.distribuidora.dto.category.CreateCategoryRequest;
import com.distribuidora.dto.category.UpdateCategoryRequest;
import com.distribuidora.exception.CategoryHasProductsException;
import com.distribuidora.exception.CategoryNotFoundException;
import com.distribuidora.exception.DuplicateCategoryException;
import com.distribuidora.repository.ProductRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CategoryService} using Mockito.
 *
 * <p>Follows the pattern established by {@link com.distribuidora.product.ProductServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository repository;

    @Mock
    CategoryMapper mapper;

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    CategoryService service;

    private static final UUID ID = UUID.randomUUID();

    // -------- Helper --------

    private Category activeCategory() {
        return Category.builder()
                .id(ID)
                .name("Verduras")
                .active(true)
                .build();
    }

    // -------- Create --------

    @Nested
    class Create {

        @Test
        void createsCategorySuccessfully() {
            CreateCategoryRequest req = new CreateCategoryRequest("Verduras");
            Category entity = activeCategory();

            when(repository.existsByName("Verduras")).thenReturn(false);
            when(mapper.toEntity(req)).thenReturn(entity);
            when(repository.save(entity)).thenReturn(entity);

            Category result = service.create(req);

            assertThat(result).isSameAs(entity);
            verify(repository).save(entity);
        }

        @Test
        void throwsDuplicateWhenNameExists() {
            CreateCategoryRequest req = new CreateCategoryRequest("Verduras");

            when(repository.existsByName("Verduras")).thenReturn(true);

            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(DuplicateCategoryException.class)
                    .hasMessageContaining("Verduras");

            verify(repository, never()).save(any());
        }

        @Test
        void createsWithValidName() {
            CreateCategoryRequest req = new CreateCategoryRequest("Frutas");
            Category entity = Category.builder()
                    .id(UUID.randomUUID())
                    .name("Frutas")
                    .active(true)
                    .build();

            when(repository.existsByName("Frutas")).thenReturn(false);
            when(mapper.toEntity(req)).thenReturn(entity);
            when(repository.save(entity)).thenReturn(entity);

            Category result = service.create(req);

            assertThat(result.getName()).isEqualTo("Frutas");
            assertThat(result.getActive()).isTrue();
        }
    }

    // -------- GetById --------

    @Nested
    class GetById {

        @Test
        void returnsCategoryWhenFound() {
            Category c = activeCategory();
            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(c));

            assertThat(service.getById(ID)).isSameAs(c);
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(ID))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));
        }
    }

    // -------- List --------

    @Test
    void listActiveReturnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Category c = activeCategory();
        Page<Category> page = new PageImpl<>(List.of(c), pageable, 1);

        when(repository.findByActiveTrue(pageable)).thenReturn(page);

        Page<Category> result = service.list(pageable, true);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listInactiveReturnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Category c = activeCategory();
        c.setActive(false);
        Page<Category> page = new PageImpl<>(List.of(c), pageable, 1);

        when(repository.findByActiveFalse(pageable)).thenReturn(page);

        Page<Category> result = service.list(pageable, false);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActive()).isFalse();
    }

    @Test
    void listWithNullActiveDefaultsToActive() {
        Pageable pageable = PageRequest.of(0, 20);
        Category c = activeCategory();
        Page<Category> page = new PageImpl<>(List.of(c), pageable, 1);

        when(repository.findByActiveTrue(pageable)).thenReturn(page);

        Page<Category> result = service.list(pageable, null);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByActiveTrue(pageable);
    }

    @Test
    void listReturnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 5);
        Category c1 = activeCategory();
        Category c2 = Category.builder().id(UUID.randomUUID()).name("Frutas").active(true).build();
        Page<Category> page = new PageImpl<>(List.of(c1, c2), pageable, 2);

        when(repository.findByActiveTrue(pageable)).thenReturn(page);

        Page<Category> result = service.list(pageable, true);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // -------- Update --------

    @Nested
    class Update {

        @Test
        void updatesCategorySuccessfully() {
            Category c = activeCategory();
            UpdateCategoryRequest req = new UpdateCategoryRequest("Legumbres");

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(c));
            when(repository.existsByNameAndIdNot("Legumbres", ID)).thenReturn(false);
            org.mockito.Mockito.doCallRealMethod().when(mapper).applyUpdate(c, req);

            Category result = service.update(ID, req);

            assertThat(result.getName()).isEqualTo("Legumbres");
        }

        @Test
        void throwsNotFoundWhenMissing() {
            UpdateCategoryRequest req = new UpdateCategoryRequest("x");

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(ID, req))
                    .isInstanceOf(CategoryNotFoundException.class);
        }

        @Test
        void throwsDuplicateWhenNameConflicts() {
            Category c = activeCategory();
            UpdateCategoryRequest req = new UpdateCategoryRequest("Frutas");

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(c));
            when(repository.existsByNameAndIdNot("Frutas", ID)).thenReturn(true);

            assertThatThrownBy(() -> service.update(ID, req))
                    .isInstanceOf(DuplicateCategoryException.class)
                    .hasMessageContaining("Frutas");
        }
    }

    // -------- Soft Delete --------

    @Nested
    class SoftDelete {

        @Test
        void softDeletesActiveCategory() {
            Category c = activeCategory();
            when(productRepository.existsByCategoryIdAndActiveTrue(ID)).thenReturn(false);
            when(repository.findById(ID)).thenReturn(Optional.of(c));

            service.softDelete(ID);

            assertThat(c.getActive()).isFalse();
        }

        @Test
        void isIdempotentForAlreadyDeleted() {
            Category c = activeCategory();
            c.setActive(false);
            when(productRepository.existsByCategoryIdAndActiveTrue(ID)).thenReturn(false);
            when(repository.findById(ID)).thenReturn(Optional.of(c));

            service.softDelete(ID);

            // Should not throw; already false so no-op
            assertThat(c.getActive()).isFalse();
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(productRepository.existsByCategoryIdAndActiveTrue(ID)).thenReturn(false);
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.softDelete(ID))
                    .isInstanceOf(CategoryNotFoundException.class);
        }

        @Test
        void throwsCategoryHasProductsWhenProductsExist() {
            when(productRepository.existsByCategoryIdAndActiveTrue(ID)).thenReturn(true);

            assertThatThrownBy(() -> service.softDelete(ID))
                    .isInstanceOf(CategoryHasProductsException.class)
                    .hasMessageContaining("Cannot deactivate category " + ID);

            verify(repository, never()).findById(any());
        }
    }

    // -------- Activate --------

    @Nested
    class Activate {

        @Test
        void activatesInactiveCategory() {
            Category c = activeCategory();
            c.setActive(false);
            when(repository.findById(ID)).thenReturn(Optional.of(c));

            Category result = service.activate(ID);

            assertThat(result.getActive()).isTrue();
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activate(ID))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }
}
