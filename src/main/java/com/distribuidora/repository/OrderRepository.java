package com.distribuidora.repository;

import com.distribuidora.model.Order;
import com.distribuidora.model.OrderStatus;
import com.distribuidora.model.OrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByDeliveryDate(LocalDate deliveryDate, Pageable pageable);

    Page<Order> findByDeliveryDateAndStatus(LocalDate deliveryDate, OrderStatus status, Pageable pageable);

    Page<Order> findByStatusIn(List<OrderStatus> statuses, Pageable pageable);

    Page<Order> findByType(OrderType type, Pageable pageable);

    long countByStatus(OrderStatus status);
}
