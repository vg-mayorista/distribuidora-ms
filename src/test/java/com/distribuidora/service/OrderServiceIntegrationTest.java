package com.distribuidora.service;

import com.distribuidora.dto.order.CreateOrderRequest;
import com.distribuidora.dto.order.OrderResponse;
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

    UUID customerId;
    UUID productId;
    UUID deliveryMethodId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
        deliveryMethodRepository.deleteAll();
        productRepository.deleteAll();

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
                .active(true)
                .build());
        deliveryMethodId = dm.getId();
    }

    private CreateOrderRequest.OrderItemRequest item(UUID pid, int packs) {
        return new CreateOrderRequest.OrderItemRequest(pid, packs);
    }

    @Test
    void createOrderComputesTotalInPhysicalUnits() {
        OrderResponse response = orderService.create(customerId, new CreateOrderRequest(
                deliveryMethodId,
                LocalDate.now().plusDays(2),
                "Calle 123", "+5411",
                "nota",
                List.of(item(productId, 2))   // 2 packs × 10 u = 20 unidades físicas
        ));

        assertThat(response.status()).isEqualTo(OrderStatus.PENDIENTE);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).packsRequested()).isEqualTo(2);
        assertThat(response.items().get(0).quantity()).isEqualTo(20); // physical units
        assertThat(response.subtotal()).isEqualByComparingTo("20000.00");  // 20 × 1000
        assertThat(response.deliveryCost()).isEqualByComparingTo("500");
        assertThat(response.total()).isEqualByComparingTo("20500.00");
        assertThat(response.editable()).isTrue();
    }

    @Test
    void transitionToArmadoDecrementsStock() {
        OrderResponse created = orderService.create(customerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productId, 3))   // 30 unidades
        ));
        int stockBefore = productRepository.findById(productId).orElseThrow().getStock();

        OrderResponse armado = orderService.transitionStatus(created.id(),
                new UpdateOrderStatusRequest(OrderStatus.ARMADO, null));

        assertThat(armado.status()).isEqualTo(OrderStatus.ARMADO);
        int stockAfter = productRepository.findById(productId).orElseThrow().getStock();
        assertThat(stockAfter).isEqualTo(stockBefore - 30);
    }

    @Test
    void invalidTransitionThrows() {
        OrderResponse created = orderService.create(customerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productId, 1))
        ));

        assertThatThrownBy(() -> orderService.transitionStatus(created.id(),
                new UpdateOrderStatusRequest(OrderStatus.ENTREGADO, null)))
            .isInstanceOf(OrderInvalidTransitionException.class);
    }

    @Test
    void cancelarArmadoRestauraStock() {
        OrderResponse created = orderService.create(customerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productId, 3))
        ));
        orderService.transitionStatus(created.id(), new UpdateOrderStatusRequest(OrderStatus.ARMADO, null));
        int stockArmado = productRepository.findById(productId).orElseThrow().getStock();

        OrderResponse cancelado = orderService.transitionStatus(created.id(),
                new UpdateOrderStatusRequest(OrderStatus.CANCELADO, "sin stock"));

        assertThat(cancelado.status()).isEqualTo(OrderStatus.CANCELADO);
        int stockCancelado = productRepository.findById(productId).orElseThrow().getStock();
        assertThat(stockCancelado).isEqualTo(stockArmado + 30);
        assertThat(cancelado.notes()).contains("sin stock");
    }

    @Test
    void fullFlowToEntregado() {
        OrderResponse created = orderService.create(customerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
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

    @Test
    void createOrderRejectsInsufficientStock() {
        Product small = productRepository.save(Product.builder()
                .name("StockBajo")
                .description("d")
                .price(new BigDecimal("100"))
                .stock(5)
                .unitsPerPack(1)
                .active(true)
                .build());

        assertThatThrownBy(() -> orderService.create(customerId, new CreateOrderRequest(
                deliveryMethodId,
                LocalDate.now().plusDays(2),
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
    void createOrderAcceptsWhenStockIsEnough() {
        OrderResponse response = orderService.create(customerId, new CreateOrderRequest(
                deliveryMethodId,
                LocalDate.now().plusDays(2),
                null, null, null,
                List.of(item(productId, 5))
        ));

        assertThat(response.status()).isEqualTo(OrderStatus.PENDIENTE);
    }
}
