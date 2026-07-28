package com.distribuidora.service;

import com.distribuidora.model.Order;
import com.distribuidora.model.OrderItem;
import com.distribuidora.model.OrderStatus;
import com.distribuidora.model.Product;
import com.distribuidora.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    public VolumeAndTicket volumeAndTicket(LocalDate from, LocalDate to) {
        Instant fromInstant = from == null ? Instant.EPOCH : from.atStartOfDay(ZONE).toInstant();
        Instant toInstant = to == null ? Instant.now() : to.plusDays(1).atStartOfDay(ZONE).toInstant();

        Object[] row = (Object[]) entityManager.createQuery(
                "SELECT COUNT(o), COALESCE(SUM(o.total), 0), COALESCE(AVG(o.total), 0) " +
                "FROM Order o " +
                "WHERE o.status = :status AND o.closedAt >= :from AND o.closedAt < :to")
            .setParameter("status", OrderStatus.ENTREGADO)
            .setParameter("from", fromInstant)
            .setParameter("to", toInstant)
            .getSingleResult();

        long orderCount = ((Number) row[0]).longValue();
        BigDecimal totalRevenue = toBigDecimal(row[1]);
        BigDecimal avgTicket = toBigDecimal(row[2]);

        Object[] deliveredRow = (Object[]) entityManager.createQuery(
                "SELECT COUNT(o), COALESCE(SUM(o.total), 0) " +
                "FROM Order o " +
                "WHERE o.status IN (:statuses) AND o.closedAt >= :from AND o.closedAt < :to")
            .setParameter("statuses", List.of(OrderStatus.ENTREGADO, OrderStatus.CANCELADO))
            .setParameter("from", fromInstant)
            .setParameter("to", toInstant)
            .getSingleResult();

        long closedCount = ((Number) deliveredRow[0]).longValue();
        BigDecimal closedRevenue = toBigDecimal(deliveredRow[1]);

        return new VolumeAndTicket(
            orderCount,
            totalRevenue,
            avgTicket,
            closedCount,
            closedRevenue
        );
    }

    public List<TopProduct> topProducts(LocalDate from, LocalDate to, int limit) {
        Instant fromInstant = from == null ? Instant.EPOCH : from.atStartOfDay(ZONE).toInstant();
        Instant toInstant = to == null ? Instant.now() : to.plusDays(1).atStartOfDay(ZONE).toInstant();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createQuery(
                "SELECT oi.productId, MAX(oi.productName), " +
                "       SUM(oi.quantity), SUM(oi.packsRequested), " +
                "       COUNT(DISTINCT o.id), SUM(oi.subtotal) " +
                "FROM OrderItem oi " +
                "JOIN oi.order o " +
                "WHERE o.status = :status AND o.closedAt >= :from AND o.closedAt < :to " +
                "GROUP BY oi.productId " +
                "ORDER BY SUM(oi.quantity) DESC")
            .setParameter("status", OrderStatus.ENTREGADO)
            .setParameter("from", fromInstant)
            .setParameter("to", toInstant)
            .setMaxResults(limit)
            .getResultList();

        return rows.stream()
            .map(r -> new TopProduct(
                (java.util.UUID) r[0],
                (String) r[1],
                ((Number) r[2]).longValue(),
                ((Number) r[3]).longValue(),
                ((Number) r[4]).longValue(),
                toBigDecimal(r[5])))
            .toList();
    }

    public List<TopCustomer> topCustomers(LocalDate from, LocalDate to, int limit) {
        Instant fromInstant = from == null ? Instant.EPOCH : from.atStartOfDay(ZONE).toInstant();
        Instant toInstant = to == null ? Instant.now() : to.plusDays(1).atStartOfDay(ZONE).toInstant();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createQuery(
                "SELECT o.userId, COUNT(o), SUM(o.total), MAX(u.firstName), MAX(u.lastName), MAX(u.email) " +
                "FROM Order o LEFT JOIN User u ON o.userId = u.id " +
                "WHERE o.status = :status AND o.closedAt >= :from AND o.closedAt < :to " +
                "GROUP BY o.userId " +
                "ORDER BY SUM(o.total) DESC")
            .setParameter("status", OrderStatus.ENTREGADO)
            .setParameter("from", fromInstant)
            .setParameter("to", toInstant)
            .setMaxResults(limit)
            .getResultList();

        return rows.stream()
            .map(r -> new TopCustomer(
                (java.util.UUID) r[0],
                ((Number) r[1]).longValue(),
                toBigDecimal(r[2]),
                (String) r[3],
                (String) r[4],
                (String) r[5]))
            .toList();
    }

    public List<LowStock> lowStock(int threshold) {
        @SuppressWarnings("unchecked")
        List<Product> products = entityManager.createQuery(
                "SELECT p FROM Product p " +
                "WHERE p.active = true AND p.stock <= :threshold " +
                "ORDER BY p.stock ASC")
            .setParameter("threshold", threshold)
            .getResultList();

        return products.stream()
            .map(p -> new LowStock(
                p.getId(),
                p.getName(),
                p.getStock(),
                p.getUnitsPerPack(),
                p.getPrice()))
            .toList();
    }

    /**
     * Some databases (notably H2) return SUM/AVG as Double; PostgreSQL returns BigDecimal.
     * Normalize to BigDecimal so the API contract is the same.
     */
    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }

    public record VolumeAndTicket(
            long deliveredCount,
            BigDecimal deliveredRevenue,
            BigDecimal avgTicket,
            long closedCount,
            BigDecimal closedRevenue
    ) {}

    public record TopProduct(
            java.util.UUID productId,
            String name,
            long unitsSold,
            long packsSold,
            long orderCount,
            BigDecimal revenue
    ) {}

    public record TopCustomer(
            java.util.UUID userId,
            long orderCount,
            BigDecimal totalSpent,
            String firstName,
            String lastName,
            String email
    ) {}

    public record LowStock(
            java.util.UUID productId,
            String name,
            Integer stock,
            Integer unitsPerPack,
            BigDecimal price
    ) {}
}
