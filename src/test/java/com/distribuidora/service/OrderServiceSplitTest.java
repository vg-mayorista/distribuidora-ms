package com.distribuidora.service;

import com.distribuidora.dto.order.CreateOrderRequest;
import com.distribuidora.dto.order.OrderResponse;
import com.distribuidora.dto.order.UpdateOrderRequest;
import com.distribuidora.exception.DeliveryMethodNotFoundException;
import com.distribuidora.exception.DeliveryWindowExpiredException;
import com.distribuidora.exception.InsufficientStockException;
import com.distribuidora.exception.MinPacksPerLineException;
import com.distribuidora.exception.MinOrderAmountException;
import com.distribuidora.exception.OrderNotEditableException;
import com.distribuidora.model.BusinessConfig;
import com.distribuidora.model.DeliveryMethod;
import com.distribuidora.model.DeliveryMethodScope;
import com.distribuidora.model.Order;
import com.distribuidora.model.OrderItem;
import com.distribuidora.model.OrderStatus;
import com.distribuidora.model.OrderType;
import com.distribuidora.model.Product;
import com.distribuidora.model.Role;
import com.distribuidora.model.User;
import com.distribuidora.repository.DeliveryMethodRepository;
import com.distribuidora.repository.OrderRepository;
import com.distribuidora.repository.ProductRepository;
import com.distribuidora.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests focused on the wholesale vs stock split in {@link OrderService}.
 * Uses Mockito for repositories and Spring-less construction.
 */
@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.parallel.Execution(org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceSplitTest {

    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock DeliveryMethodRepository deliveryMethodRepository;
    @Mock UserRepository userRepository;
    @Mock BusinessConfigService businessConfigService;
    @Mock DeliveryScheduleService schedule;

    UUID userId;
    UUID productId;
    UUID deliveryMethodId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        deliveryMethodId = UUID.randomUUID();

        when(businessConfigService.getOrInitConfig()).thenReturn(BusinessConfig.builder()
                .minPacksPerLine(1)
                .minOrderAmount(new BigDecimal("100.00"))
                .updatedAt(Instant.now())
                .build());

        User customer = User.builder()
                .id(userId)
                .email("u@e.com")
                .firstName("Cliente")
                .lastName("Demo")
                .role(Role.builder().name("ROLE_CUSTOMER").build())
                .active(true)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(customer));

        DeliveryMethod dm = DeliveryMethod.builder()
                .id(deliveryMethodId)
                .name("Envío a Domicilio")
                .cost(new BigDecimal("500"))
                .appliesToOrderType(DeliveryMethodScope.BOTH)
                .active(true)
                .build();
        when(deliveryMethodRepository.findByIdAndActiveTrue(deliveryMethodId))
                .thenReturn(Optional.of(dm));

        Product product = Product.builder()
                .id(productId)
                .name("Yerba Mate 1kg")
                .description("d")
                .price(new BigDecimal("1000"))
                .stock(100)
                .unitsPerPack(10)
                .active(true)
                .build();
        when(productRepository.findByIdAndActiveTrue(productId))
                .thenReturn(Optional.of(product));
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
    }

    private OrderService buildService() {
        return new OrderService(orderRepository, productRepository,
                deliveryMethodRepository, userRepository,
                businessConfigService, schedule);
    }

    private CreateOrderRequest.OrderItemRequest item(UUID pid, int packs) {
        return new CreateOrderRequest.OrderItemRequest(pid, packs);
    }

    @Test
    void createStock_decrementsStockAndSetsTypeStock() {
        OrderService svc = buildService();
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.getItems().forEach(i -> i.setId(UUID.randomUUID()));
            return o;
        });

        OrderResponse resp = svc.createStock(userId, new CreateOrderRequest(
                deliveryMethodId, null, "CABA 123", "+5411", null,
                List.of(item(productId, 2))
        ));

        assertThat(resp.type()).isEqualTo(OrderType.STOCK);
        assertThat(resp.status()).isEqualTo(OrderStatus.PENDIENTE);
        assertThat(resp.deliveryDate()).isNull();
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    void createStock_rejectsWhenInsufficientStock() {
        // product stock inicial = 100 unidades; pedimos 50 packs × 10 u = 500 unidades
        OrderService svc = buildService();
        assertThatThrownBy(() -> svc.createStock(userId, new CreateOrderRequest(
                deliveryMethodId, null, null, null, null,
                List.of(item(productId, 50))
        )))
                .isInstanceOf(InsufficientStockException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createStock_rejectsDeliveryDate() {
        OrderService svc = buildService();
        assertThatThrownBy(() -> svc.createStock(userId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(1), null, null, null,
                List.of(item(productId, 1))
        )))
                .isInstanceOf(OrderNotEditableException.class)
                .hasMessageContaining("stock");
    }

    @Test
    void createWholesale_doesNotDecrementStockButRequiresDate() {
        OrderService svc = buildService();
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schedule.isWithinWindow(any(LocalDate.class))).thenReturn(true);

        LocalDate wed = LocalDate.now().plusDays(2);
        OrderResponse resp = svc.createWholesale(userId, new CreateOrderRequest(
                deliveryMethodId, wed, "CABA 123", "+5411", null,
                List.of(item(productId, 2))
        ));

        assertThat(resp.type()).isEqualTo(OrderType.WHOLESALE);
        assertThat(resp.deliveryDate()).isEqualTo(wed);
        verify(productRepository, never()).findById(any());     // nunca decrementa
    }

    @Test
    void createWholesale_rejectsWhenCutoffPassed() {
        OrderService svc = buildService();
        when(schedule.isWithinWindow(any(LocalDate.class))).thenReturn(false);

        assertThatThrownBy(() -> svc.createWholesale(userId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productId, 1))
        )))
                .isInstanceOf(DeliveryWindowExpiredException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createWholesale_rejectsMissingDeliveryDate() {
        OrderService svc = buildService();
        assertThatThrownBy(() -> svc.createWholesale(userId, new CreateOrderRequest(
                deliveryMethodId, null, null, null, null,
                List.of(item(productId, 1))
        )))
                .isInstanceOf(OrderNotEditableException.class);
    }

    @Test
    void createDispatchesToCreateWholesaleWhenDatePresent() {
        OrderService svc = buildService();
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schedule.isWithinWindow(any(LocalDate.class))).thenReturn(true);

        OrderResponse resp = svc.create(userId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productId, 2))
        ));
        assertThat(resp.type()).isEqualTo(OrderType.WHOLESALE);
    }

    @Test
    void createDispatchesToCreateStockWhenDateNull() {
        OrderService svc = buildService();
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        // Override: usar "Retiro en Local" para que el date null sea válido en stock.
        when(deliveryMethodRepository.findByIdAndActiveTrue(deliveryMethodId))
                .thenReturn(Optional.of(DeliveryMethod.builder()
                        .id(deliveryMethodId)
                        .name("Envío Express")
                        .cost(new BigDecimal("1200"))
                        .appliesToOrderType(DeliveryMethodScope.STOCK)
                        .active(true)
                        .build()));

        OrderResponse resp = svc.create(userId, new CreateOrderRequest(
                deliveryMethodId, null, null, null, null,
                List.of(item(productId, 2))
        ));
        assertThat(resp.type()).isEqualTo(OrderType.STOCK);
    }

    @Test
    void createRejectsMissingDeliveryMethod() {
        OrderService svc = buildService();
        UUID unknown = UUID.randomUUID();
        when(deliveryMethodRepository.findByIdAndActiveTrue(unknown))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.create(userId, new CreateOrderRequest(
                unknown, null, null, null, null,
                List.of(item(productId, 2))
        )))
                .isInstanceOf(DeliveryMethodNotFoundException.class);
    }

    @Test
    void createRejectsEmptyItems() {
        OrderService svc = buildService();
        assertThatThrownBy(() -> svc.create(userId, new CreateOrderRequest(
                deliveryMethodId, null, null, null, null,
                List.of()
        )))
                .isInstanceOf(OrderNotEditableException.class);
    }

    @Test
    void createRejectsBelowMinPacksPerLine() {
        OrderService svc = buildService();
        when(businessConfigService.getOrInitConfig()).thenReturn(BusinessConfig.builder()
                .minPacksPerLine(99)
                .minOrderAmount(new BigDecimal("100.00"))
                .updatedAt(Instant.now())
                .build());
        assertThatThrownBy(() -> svc.createStock(userId, new CreateOrderRequest(
                deliveryMethodId, null, null, null, null,
                List.of(item(productId, 2))
        )))
                .isInstanceOf(MinPacksPerLineException.class);
    }

    @Test
    void createRejectsBelowMinOrderAmount() {
        OrderService svc = buildService();
        // setUp deja minPacksPerLine=1 y 2 packs a $1000 = $2000 < $50.000
        when(businessConfigService.getOrInitConfig()).thenReturn(BusinessConfig.builder()
                .minPacksPerLine(1)
                .minOrderAmount(new BigDecimal("50000.00"))
                .updatedAt(Instant.now())
                .build());
        assertThatThrownBy(() -> svc.createStock(userId, new CreateOrderRequest(
                deliveryMethodId, null, null, null, null,
                List.of(item(productId, 2))
        )))
                .isInstanceOf(MinOrderAmountException.class);
    }

    @Test
    void cancelStockRestoresStockButCancelWholesaleDoesNot() {
        // Wholesale order has stockDecremented=false, cancel must not touch stock.
        Order orderWholesale = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.PENDIENTE)
                .type(OrderType.WHOLESALE)
                .deliveryMethodId(deliveryMethodId)
                .deliveryMethodName("Envío a Domicilio")
                .deliveryCost(new BigDecimal("500"))
                .deliveryDate(LocalDate.now().plusDays(2))
                .subtotal(new BigDecimal("2000"))
                .total(new BigDecimal("2500"))
                .stockDecremented(Boolean.FALSE)
                .build();
        orderWholesale.getItems().add(OrderItem.builder()
                .id(UUID.randomUUID())
                .order(orderWholesale)
                .productId(productId)
                .productName("Yerba Mate 1kg")
                .packsRequested(2)
                .unitsPerPackAtOrder(10)
                .quantity(20)
                .unitPrice(new BigDecimal("1000"))
                .subtotal(new BigDecimal("2000"))
                .build());
        when(orderRepository.findById(orderWholesale.getId())).thenReturn(Optional.of(orderWholesale));
        when(schedule.isWithinWindow(any(LocalDate.class))).thenReturn(true);

        OrderService svc = buildService();
        svc.cancelMine(userId, orderWholesale.getId());

        verify(productRepository, never()).findById(any());
        assertThat(orderWholesale.getStatus()).isEqualTo(OrderStatus.CANCELADO);
    }

    @Test
    void updateStock_rejectsDeliveryDate() {
        Order orderStock = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.PENDIENTE)
                .type(OrderType.STOCK)
                .deliveryMethodId(deliveryMethodId)
                .deliveryMethodName("Envío a Domicilio")
                .deliveryCost(new BigDecimal("500"))
                .subtotal(new BigDecimal("2000"))
                .total(new BigDecimal("2500"))
                .stockDecremented(Boolean.FALSE)
                .build();
        orderStock.getItems().add(OrderItem.builder()
                .id(UUID.randomUUID())
                .order(orderStock)
                .productId(productId)
                .productName("Yerba Mate 1kg")
                .packsRequested(2)
                .unitsPerPackAtOrder(10)
                .quantity(20)
                .unitPrice(new BigDecimal("1000"))
                .subtotal(new BigDecimal("2000"))
                .build());
        when(orderRepository.findById(orderStock.getId())).thenReturn(Optional.of(orderStock));

        OrderService svc = buildService();
        UpdateOrderRequest.OrderItemRequest updateItem =
                new UpdateOrderRequest.OrderItemRequest(productId, 2);
        assertThatThrownBy(() -> svc.updateMine(userId, orderStock.getId(),
                new UpdateOrderRequest(deliveryMethodId, LocalDate.now().plusDays(1),
                        null, null, null, List.of(updateItem))))
                .isInstanceOf(OrderNotEditableException.class);
    }
}
