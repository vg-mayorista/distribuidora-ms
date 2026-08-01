package com.distribuidora.service;

import com.distribuidora.dto.order.CreateOrderRequest;
import com.distribuidora.dto.order.OrderResponse;
import com.distribuidora.dto.order.UpdateOrderStatusRequest;
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

@SpringBootTest
@ActiveProfiles("h2")
class ReportServiceIntegrationTest {

    @Autowired ReportService reportService;
    @Autowired OrderService orderService;
    @Autowired ProductRepository productRepository;
    @Autowired DeliveryMethodRepository deliveryMethodRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;

    UUID customerId;
    UUID otherCustomerId;
    UUID productAId;
    UUID productBId;
    UUID deliveryMethodId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
        deliveryMethodRepository.deleteAll();
        productRepository.deleteAll();

        Role role = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() ->
            roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("c").build()));

        User c1 = userRepository.save(User.builder()
                .email("c1@example.com").password("x").firstName("Ana").lastName("García")
                .role(role).active(true).build());
        customerId = c1.getId();

        User c2 = userRepository.save(User.builder()
                .email("c2@example.com").password("x").firstName("Luis").lastName("Pérez")
                .role(role).active(true).build());
        otherCustomerId = c2.getId();

        Product a = productRepository.save(Product.builder()
                .name("Yerba").description("d").price(new BigDecimal("1000"))
                .stock(500).unitsPerPack(10).active(true).build());
        productAId = a.getId();

        Product b = productRepository.save(Product.builder()
                .name("Azúcar").description("d").price(new BigDecimal("500"))
                .stock(500).unitsPerPack(1).active(true).build());
        productBId = b.getId();

        DeliveryMethod dm = deliveryMethodRepository.save(DeliveryMethod.builder()
                .name("Envío").cost(new BigDecimal("100")).active(true).build());
        deliveryMethodId = dm.getId();
    }

    private CreateOrderRequest.OrderItemRequest item(UUID pid, int packs) {
        return new CreateOrderRequest.OrderItemRequest(pid, packs);
    }

    private OrderResponse placeAndDeliver(UUID customer, CreateOrderRequest req) {
        OrderResponse created = orderService.create(customer, req);
        orderService.transitionStatus(created.id(), new UpdateOrderStatusRequest(OrderStatus.ARMADO, null));
        orderService.transitionStatus(created.id(), new UpdateOrderStatusRequest(OrderStatus.ENVIADO, null));
        orderService.transitionStatus(created.id(), new UpdateOrderStatusRequest(OrderStatus.ENTREGADO, null));
        return orderService.get(created.id());
    }

    @Test
    void volumeCountsDeliveredOrders() {
        placeAndDeliver(customerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productAId, 2))));

        placeAndDeliver(otherCustomerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productBId, 5))));

        ReportService.VolumeAndTicket v = reportService.volumeAndTicket(null, null);

        assertThat(v.deliveredCount()).isEqualTo(2);
        // order 1: 2 packs × 1000 = 2000 + delivery 100 = 2100
        // order 2: 5 packs × 500 = 2500 + delivery 100 = 2600
        assertThat(v.deliveredRevenue()).isEqualByComparingTo("4700.00");
        assertThat(v.avgTicket()).isEqualByComparingTo("2350.00");
    }

    @Test
    void topProductsAggregatesByProduct() {
        placeAndDeliver(customerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productAId, 3))));  // 30 units

        placeAndDeliver(otherCustomerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productAId, 2), item(productBId, 5))));  // 20 + 5 units

        List<ReportService.TopProduct> top = reportService.topProducts(null, null, 10);

        ReportService.TopProduct yerba = top.stream().filter(t -> t.name().equals("Yerba")).findFirst().orElseThrow();
        assertThat(yerba.unitsSold()).isEqualTo(50);
        assertThat(yerba.packsSold()).isEqualTo(5);
        assertThat(yerba.orderCount()).isEqualTo(2);

        ReportService.TopProduct azucar = top.stream().filter(t -> t.name().equals("Azúcar")).findFirst().orElseThrow();
        assertThat(azucar.unitsSold()).isEqualTo(5);
    }

    @Test
    void topCustomersAggregatesByUser() {
        placeAndDeliver(customerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productAId, 1))));

        placeAndDeliver(customerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productBId, 2))));

        placeAndDeliver(otherCustomerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productAId, 1))));

        List<ReportService.TopCustomer> top = reportService.topCustomers(null, null, 10);

        ReportService.TopCustomer first = top.get(0);
        assertThat(first.userId()).isEqualTo(customerId);
        assertThat(first.orderCount()).isEqualTo(2);
        assertThat(first.firstName()).isEqualTo("Ana");

        ReportService.TopCustomer second = top.get(1);
        assertThat(second.userId()).isEqualTo(otherCustomerId);
        assertThat(second.orderCount()).isEqualTo(1);
    }

    @Test
    void lowStockReturnsProductsBelowThreshold() {
        productRepository.findById(productAId).ifPresent(p -> { p.setStock(3); productRepository.save(p); });
        productRepository.findById(productBId).ifPresent(p -> { p.setStock(50); productRepository.save(p); });

        List<ReportService.LowStock> low = reportService.lowStock(10);

        assertThat(low).hasSize(1);
        assertThat(low.get(0).name()).isEqualTo("Yerba");
        assertThat(low.get(0).stock()).isEqualTo(3);
    }

    @Test
    void cancelledOrdersDoNotCountInVolume() {
        OrderResponse created = orderService.create(customerId, new CreateOrderRequest(
                deliveryMethodId, LocalDate.now().plusDays(2), null, null, null,
                List.of(item(productAId, 1))));
        orderService.transitionStatus(created.id(), new UpdateOrderStatusRequest(OrderStatus.CANCELADO, null));

        ReportService.VolumeAndTicket v = reportService.volumeAndTicket(null, null);
        assertThat(v.deliveredCount()).isEqualTo(0);
    }
}
