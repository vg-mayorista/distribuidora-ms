package com.distribuidora.service;

import com.distribuidora.config.StockProperties;
import com.distribuidora.dto.product.StockStatus;
import com.distribuidora.model.Product;
import com.distribuidora.repository.ProductRepository;
import com.distribuidora.repository.CategoryRepository;
import com.distribuidora.exception.CategoryNotFoundException;
import com.distribuidora.exception.DuplicateProductException;
import com.distribuidora.exception.ProductNotFoundException;
import com.distribuidora.dto.product.CreateProductRequest;
import com.distribuidora.dto.product.PatchProductRequest;
import com.distribuidora.dto.product.UpdateProductRequest;
import com.distribuidora.mapper.ProductMapper;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repository;

    @Mock
    ProductMapper mapper;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    StockProperties stockProperties;

    @InjectMocks
    ProductService service;

    private static final UUID ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID NONEXISTENT_CATEGORY_ID = UUID.randomUUID();

    private Product activeProduct() {
        return Product.builder()
                .id(ID)
                .categoryId(CATEGORY_ID)
                .name("Manzana")
                .description("desc")
                .price(new BigDecimal("100.00"))
                .stock(10)
                .unitsPerPack(1)
                .active(true)
                .build();
    }

    @Nested
    class Create {
        @Test
        void createsProductSuccessfully() {
            CreateProductRequest req = new CreateProductRequest(CATEGORY_ID, null, "Manzana", "desc", new BigDecimal("100.00"), 10, 12, null);
            Product entity = activeProduct();

            when(categoryRepository.existsByIdAndActiveTrue(CATEGORY_ID)).thenReturn(true);
            when(repository.existsByName("Manzana")).thenReturn(false);
            when(mapper.toEntity(req)).thenReturn(entity);
            when(repository.save(entity)).thenReturn(entity);

            Product result = service.create(req);

            assertThat(result).isSameAs(entity);
            verify(repository).save(entity);
        }

        @Test
        void throwsDuplicateWhenNameExists() {
            CreateProductRequest req = new CreateProductRequest(CATEGORY_ID, null, "Manzana", "desc", new BigDecimal("100.00"), 10, 12, null);

            when(categoryRepository.existsByIdAndActiveTrue(CATEGORY_ID)).thenReturn(true);
            when(repository.existsByName("Manzana")).thenReturn(true);

            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(DuplicateProductException.class)
                    .hasMessageContaining("Manzana");

            verify(repository, never()).save(any());
        }

        @Test
        void throwsCategoryNotFoundWhenCategoryDoesNotExist() {
            CreateProductRequest req = new CreateProductRequest(NONEXISTENT_CATEGORY_ID, null, "Valid", "desc", new BigDecimal("100.00"), 10, 12, null);

            when(categoryRepository.existsByIdAndActiveTrue(NONEXISTENT_CATEGORY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(mapper, never()).toEntity(any());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class GetById {
        @Test
        void returnsProductWhenFound() {
            Product p = activeProduct();
            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));

            assertThat(service.getById(ID)).isSameAs(p);
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(ID))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Test
    void listReturnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Product p = activeProduct();
        Page<Product> page = new PageImpl<>(List.of(p), pageable, 1);

        when(repository.findByActiveTrue(pageable)).thenReturn(page);

        Page<Product> result = service.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Nested
    class Update {
        @Test
        void updatesProductSuccessfully() {
            Product p = activeProduct();
            UpdateProductRequest req = new UpdateProductRequest(CATEGORY_ID, null, "Lechuga", "", new BigDecimal("50.00"), 5, 12, 20);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));
            when(categoryRepository.existsByIdAndActiveTrue(CATEGORY_ID)).thenReturn(true);
            when(repository.existsByNameAndIdNot("Lechuga", ID)).thenReturn(false);
            org.mockito.Mockito.doCallRealMethod().when(mapper).applyUpdate(p, req);

            Product result = service.update(ID, req);

            assertThat(result.getName()).isEqualTo("Lechuga");
            assertThat(result.getPrice()).isEqualByComparingTo("50.00");
            assertThat(result.getStock()).isEqualTo(5);
            assertThat(result.getUnitsPerPack()).isEqualTo(12);
        }

        @Test
        void throwsNotFoundWhenMissing() {
            UpdateProductRequest req = new UpdateProductRequest(CATEGORY_ID, null, "x", "", new BigDecimal("1"), 1, 1, 20);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(ID, req))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        void throwsDuplicateWhenNameConflicts() {
            Product p = activeProduct();
            UpdateProductRequest req = new UpdateProductRequest(CATEGORY_ID, null, "Tomate", "", new BigDecimal("50.00"), 5, 1, 20);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));
            when(categoryRepository.existsByIdAndActiveTrue(CATEGORY_ID)).thenReturn(true);
            when(repository.existsByNameAndIdNot("Tomate", ID)).thenReturn(true);

            assertThatThrownBy(() -> service.update(ID, req))
                    .isInstanceOf(DuplicateProductException.class)
                    .hasMessageContaining("Tomate");
        }
    }

    @Nested
    class Patch {
        @Test
        void patchesSingleField() {
            Product p = activeProduct();
            PatchProductRequest req = new PatchProductRequest(null, null, null, null, new BigDecimal("200.00"), null, null, null);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));
            org.mockito.Mockito.doCallRealMethod().when(mapper).applyPatch(p, req);

            Product result = service.patch(ID, req);

            assertThat(result.getPrice()).isEqualByComparingTo("200.00");
            assertThat(result.getName()).isEqualTo("Manzana");
        }

        @Test
        void patchesNothingWhenAllFieldsNull() {
            Product p = activeProduct();
            PatchProductRequest req = new PatchProductRequest(null, null, null, null, null, null, null, null);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));

            Product result = service.patch(ID, req);

            assertThat(result.getName()).isEqualTo("Manzana");
            assertThat(result.getPrice()).isEqualByComparingTo("100.00");
        }

        @Test
        void patchesStock() {
            Product p = activeProduct();
            PatchProductRequest req = new PatchProductRequest(null, null, null, null, null, 15, null, null);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));
            org.mockito.Mockito.doCallRealMethod().when(mapper).applyPatch(p, req);

            Product result = service.patch(ID, req);

            assertThat(result.getStock()).isEqualTo(15);
        }

        @Test
        void throwsBadRequestWhenNameIsBlank() {
            Product p = activeProduct();
            PatchProductRequest req = new PatchProductRequest(null, null, "   ", null, null, null, null, null);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.patch(ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }

        @Test
        void throwsBadRequestWhenStockNegative() {
            Product p = activeProduct();
            PatchProductRequest req = new PatchProductRequest(null, null, null, null, null, -1, null, null);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.patch(ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }

        @Test
        void throwsBadRequestWhenUnitsPerPackLessThanOne() {
            Product p = activeProduct();
            PatchProductRequest req = new PatchProductRequest(null, null, null, null, null, null, 0, null);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.patch(ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }

        @Test
        void throwsBadRequestWhenLowStockThresholdNegative() {
            Product p = activeProduct();
            PatchProductRequest req = new PatchProductRequest(null, null, null, null, null, null, null, -1);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.patch(ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }

        @Test
        void patchesLowStockThreshold() {
            Product p = activeProduct();
            PatchProductRequest req = new PatchProductRequest(null, null, null, null, null, null, null, 50);

            when(repository.findByIdAndActiveTrue(ID)).thenReturn(Optional.of(p));
            org.mockito.Mockito.doCallRealMethod().when(mapper).applyPatch(p, req);

            Product result = service.patch(ID, req);

            assertThat(result.getLowStockThreshold()).isEqualTo(50);
        }
    }

    @Nested
    class SoftDelete {
        @Test
        void softDeletesActiveProduct() {
            Product p = activeProduct();
            when(repository.findById(ID)).thenReturn(Optional.of(p));

            service.softDelete(ID);

            assertThat(p.getActive()).isFalse();
        }

        @Test
        void isIdempotentForAlreadyDeleted() {
            Product p = activeProduct();
            p.setActive(false);
            when(repository.findById(ID)).thenReturn(Optional.of(p));

            service.softDelete(ID);

            assertThat(p.getActive()).isFalse();
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.softDelete(ID))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    class Activate {
        @Test
        void activatesInactiveProduct() {
            Product p = activeProduct();
            p.setActive(false);
            when(repository.findById(ID)).thenReturn(Optional.of(p));

            Product result = service.activate(ID);

            assertThat(result.getActive()).isTrue();
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activate(ID))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    class ComputeStatus {
        @Test
        void outOfStockWhenStockIsZero() {
            Product p = activeProduct();
            p.setStock(0);
            p.setLowStockThreshold(20);

            assertThat(service.computeStatus(p)).isEqualTo(StockStatus.OUT_OF_STOCK);
        }

        @Test
        void outOfStockWhenStockIsNull() {
            Product p = activeProduct();
            p.setStock(null);
            p.setLowStockThreshold(20);

            assertThat(service.computeStatus(p)).isEqualTo(StockStatus.OUT_OF_STOCK);
        }

        @Test
        void lowStockWhenAtOrBelowThreshold() {
            Product p = activeProduct();
            p.setStock(20);
            p.setLowStockThreshold(20);

            assertThat(service.computeStatus(p)).isEqualTo(StockStatus.LOW_STOCK);
        }

        @Test
        void lowStockWhenBelowThreshold() {
            Product p = activeProduct();
            p.setStock(5);
            p.setLowStockThreshold(20);

            assertThat(service.computeStatus(p)).isEqualTo(StockStatus.LOW_STOCK);
        }

        @Test
        void inStockWhenAboveThreshold() {
            Product p = activeProduct();
            p.setStock(50);
            p.setLowStockThreshold(20);

            assertThat(service.computeStatus(p)).isEqualTo(StockStatus.IN_STOCK);
        }

        @Test
        void usesDefaultThresholdWhenProductThresholdIsNull() {
            Product p = activeProduct();
            p.setStock(15);
            p.setLowStockThreshold(null);

            when(stockProperties.getDefaultLowThreshold()).thenReturn(20);

            assertThat(service.computeStatus(p)).isEqualTo(StockStatus.LOW_STOCK);
        }

        @Test
        void productThresholdOverridesDefault() {
            Product p = activeProduct();
            p.setStock(50);
            p.setLowStockThreshold(60);

            assertThat(service.computeStatus(p)).isEqualTo(StockStatus.LOW_STOCK);
        }
    }
}
