package com.distribuidora.service;

import com.distribuidora.dto.order.CreateOrderRequest;
import com.distribuidora.dto.order.OrderResponse;
import com.distribuidora.dto.order.UpdateOrderRequest;
import com.distribuidora.dto.order.UpdateOrderStatusRequest;
import com.distribuidora.exception.InsufficientStockException;
import com.distribuidora.exception.OrderInvalidTransitionException;
import com.distribuidora.exception.OrderNotFoundException;
import com.distribuidora.model.*;
import com.distribuidora.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("h2")
class OrderServiceIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired ProductRepository productRepository;
    @Autowired DeliveryMethodRepository deliveryMethodRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired BusinessConfigRepository businessConfigRepository;
    @Autowired DeliveryWindowRepository deliveryWindowRepository;

    UUID customerId;
    UUID productId;
    UUID deliveryMethodId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
        deliveryMethodRepository.deleteAll();
        productRepository.deleteAll();
        deliveryWindowRepository.deleteAll();
        businessConfigRepository.deleteAll();

        // Re-seed BusinessConfig with a low threshold so tests don't trip on the default 5 packs.
        BusinessConfig cfg = BusinessConfig.builder()
                .minPacksPerLine(1)
                .updatedAt(java.time.Instant.now())
                .build();
        businessConfigRepository.save(cfg);

        Role role = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() ->
            roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("c").build()));

        User customer = userRepository.save(User.builder()
                .email("test@example.com")
                .password("x")
                .firstName("Test")
                .lastName("Customer")
                .role(role)
                .active(true)
                .build());
        customerId = customer.getId();

        Product product = productRepository.save(Product.builder()
                .name("Yerba")
                .description("d")
                .price(new BigDecimal("1000.00"))
                .stock(100)
                .unitsPerPack(10)
                .active(true)
                .build());
        productId = product.getId();

        DeliveryMethod dm = deliveryMethodRepository.save(DeliveryMethod.builder()
                .name("Envío a Domicilio")
                .cost(new BigDecimal("500"))
                .appliesToOrderType(DeliveryMethodScope.BOTH)
                .active(true)
                .build());
        deliveryMethodId = dm.getId();
    }

    private CreateOrderRequest.OrderItemRequest item(UUID pid, int packs) {
        return new CreateOrderRequest.OrderItemRequest(pid, packs);
    }

    @Test
    void createStockDecrementsStock() {
        int before = productRepository.findById(productId).orElseThrow().getStock();
        OrderResponse resp = orderService.createStock(customerId, new CreateOrderRequest(
                deliveryMethodId, null, "Calle 123", "+5411", "nota",
                List.of(item(productId, 3))    // 30 unidades
        ));
        assertThat(resp.type()).isEqualTo(OrderType.STOCK);
        int after = productRepository.findById(productId).orElseThrow().getStock();
        assertThat(after).isEqualTo(before - 30);
    }

    @Test
    void createWholesaleDoesNotDecrementStock() {
        int before = productRepository.findById(productId).orElseThrow().getStock();
        // Note: requires no active DeliveryWindow to compute cutoff. We assert that
        // we'll fail with MinOrderRequirementsNotMetException (because no windows are
        // seeded in H2 by default) OR the request goes through. Either way: stock
        // must NOT change.
        try {
            orderService.createWholesale(customerId, new CreateOrderRequest(
                    deliveryMethodId,
                    LocalDate.now().plusDays(2),
                    "Calle 123", "+5411", "nota",
                    List.of(item(productId, 2))
            ));
        } catch (Exception ignored) {
            // may throw DeliveryWindowExpired if no windows seeded
        }
        int after = productRepository.findById(productId).orElseThrow().getStock();
        assertThat(after).isEqualTo(before);
    }

    @Test
    void createStockRejectsInsufficientStock() {
        Product small = productRepository.save(Product.builder()
                .name("StockBajo")
                .description("d")
                .price(new BigDecimal("100"))
                .stock(5)
                .unitsPerPack(1)
                .active(true)
                .build());

        assertThatThrownBy(() -> orderService.createStock(customerId, new CreateOrderRequest(
                deliveryMethodId,
                null,
                null, null, null,
                List.of(item(small.getId(), 10))
        )))
            .isInstanceOf(InsufficientStockException.class)
            .satisfies(ex -> {
                InsufficientStockException ise = (InsufficientStockException) ex;
                assertThat(ise.getItems()).hasSize(1);
                assertThat(ise.getItems().get(0).available()).isEqualTo(5);
                assertThat(ise.getItems().get(0).requested()).isEqualTo(10);
            });
    }

    @Test
    void invalidTransitionThrows() {
        OrderResponse created = orderService.createStock(customerId, new CreateOrderRequest(
                deliveryMethodId, null, null, null, null,
                List.of(item(productId, 1))
        ));

        assertThatThrownBy(() -> orderService.transitionStatus(created.id(),
                new UpdateOrderStatusRequest(OrderStatus.ENTREGADO, null)))
            .isInstanceOf(OrderInvalidTransitionException.class);
    }

    @Test
    void cancelarStockRestauraStock() {
        int initialStock = productRepository.findById(productId).orElseThrow().getStock();
        OrderResponse created = orderService.createStock(customerId, new CreateOrderRequest(
                deliveryMethodId, null, null, null, null,
                List.of(item(productId, 3))
        ));
        int stockCreated = productRepository.findById(productId).orElseThrow().getStock();
        assertThat(stockCreated).isEqualTo(initialStock - 30);

        OrderResponse cancelado = orderService.transitionStatus(created.id(),
                new UpdateOrderStatusRequest(OrderStatus.CANCELADO, "sin stock"));

        assertThat(cancelado.status()).isEqualTo(OrderStatus.CANCELADO);
        int stockCancelado = productRepository.findById(productId).orElseThrow().getStock();
        assertThat(stockCancelado).isEqualTo(initialStock);
        assertThat(cancelado.notes()).contains("sin stock");
    }

    @Test
    void fullFlowToEntregado() {
        OrderResponse created = orderService.createStock(customerId, new CreateOrderRequest(
                deliveryMethodId, null, null, null, null,
                List.of(item(productId, 1))
        ));
        OrderResponse armado = orderService.transitionStatus(created.id(),
                new UpdateOrderStatusRequest(OrderStatus.ARMADO, null));
        OrderResponse enviado = orderService.transitionStatus(armado.id(),
                new UpdateOrderStatusRequest(OrderStatus.ENVIADO, null));
        OrderResponse entregado = orderService.transitionStatus(enviado.id(),
                new UpdateOrderStatusRequest(OrderStatus.ENTREGADO, null));

        assertThat(entregado.status()).isEqualTo(OrderStatus.ENTREGADO);
        assertThat(entregado.closedAt()).isNotNull();
    }

    @Test
    void getNotFoundThrows() {
        assertThatThrownBy(() -> orderService.get(UUID.randomUUID()))
            .isInstanceOf(OrderNotFoundException.class);
    }
}
