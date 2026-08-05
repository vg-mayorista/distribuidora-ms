package com.distribuidora.service;

import com.distribuidora.dto.order.CreateOrderRequest;
import com.distribuidora.dto.order.OrderItemResponse;
import com.distribuidora.dto.order.OrderResponse;
import com.distribuidora.dto.order.UpdateOrderRequest;
import com.distribuidora.dto.order.UpdateOrderStatusRequest;
import com.distribuidora.exception.DeliveryMethodNotFoundException;
import com.distribuidora.exception.DeliveryWindowExpiredException;
import com.distribuidora.exception.InsufficientStockException;
import com.distribuidora.exception.OrderInvalidTransitionException;
import com.distribuidora.exception.OrderNotEditableException;
import com.distribuidora.exception.OrderNotFoundException;
import com.distribuidora.exception.ProductNotFoundException;
import com.distribuidora.exception.MinPacksPerLineException;
import com.distribuidora.model.BusinessConfig;
import com.distribuidora.model.DeliveryMethod;
import com.distribuidora.model.Order;
import com.distribuidora.model.OrderItem;
import com.distribuidora.model.OrderStatus;
import com.distribuidora.model.OrderType;
import com.distribuidora.model.Product;
import com.distribuidora.model.User;
import com.distribuidora.repository.DeliveryMethodRepository;
import com.distribuidora.repository.OrderRepository;
import com.distribuidora.repository.ProductRepository;
import com.distribuidora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lógica de pedidos. Soporta dos flujos:
 * <ul>
 *   <li>{@link OrderType#WHOLESALE WHOLESALE}: pedido al catálogo general; no toca stock;
 *       requiere {@code deliveryDate} dentro de una ventana semanal abierta.</li>
 *   <li>{@link OrderType#STOCK STOCK}: pedido del excedente en depósito; descuenta stock al
 *       confirmar y lo restaura al cancelar.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DeliveryMethodRepository deliveryMethodRepository;
    private final UserRepository userRepository;
    private final BusinessConfigService businessConfigService;
    private final DeliveryScheduleService deliveryScheduleService;

    private final Clock clock = Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));

    // ── Queries ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OrderResponse> listMine(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMine(UUID userId, UUID orderId) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!o.getUserId().equals(userId)) {
            throw new OrderNotFoundException(orderId);
        }
        return toResponse(o);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID orderId) {
        return toResponse(loadOrThrow(orderId));
    }

    @Transactional(readOnly = true)
    public OrderResponse toResponsePublic(Order order) {
        return toResponse(order);
    }

    // ── Create ───────────────────────────────────────────────────────────

    /**
     * Compatibilidad hacia atrás: dispatch por presencia de {@code deliveryDate}.
     * <ul>
     *   <li>{@code deliveryDate != null} → {@link #createWholesale}</li>
     *   <li>{@code deliveryDate == null} → {@link #createStock}</li>
     * </ul>
     */
    public OrderResponse create(UUID userId, CreateOrderRequest req) {
        if (req.deliveryDate() == null) {
            return createStock(userId, req);
        }
        return createWholesale(userId, req);
    }

    /**
     * Pedido mayorista a fábrica.
     * <p>{@code deliveryDate} debe existir y estar dentro de una ventana abierta
     * ({@link DeliveryScheduleService#isWithinWindow}). No valida ni descuenta stock.
     */
    public OrderResponse createWholesale(UUID userId, CreateOrderRequest req) {
        validateItemsPresent(req);
        if (req.deliveryDate() == null) {
            throw new OrderNotEditableException(
                    "Pedido mayorista requiere fecha de entrega (deliveryDate).");
        }
        assertDeliveryDateInWindow(req.deliveryDate());

        DeliveryMethod dm = deliveryMethodRepository.findByIdAndActiveTrue(req.deliveryMethodId())
                .orElseThrow(() -> new DeliveryMethodNotFoundException(req.deliveryMethodId()));

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDIENTE)
                .type(OrderType.WHOLESALE)
                .deliveryMethodId(dm.getId())
                .deliveryMethodName(dm.getName())
                .deliveryCost(dm.getCost())
                .deliveryAddress(req.deliveryAddress())
                .deliveryPhone(req.deliveryPhone())
                .notes(req.notes())
                .deliveryDate(req.deliveryDate())
                .stockDecremented(Boolean.FALSE)
                .build();

        Map<UUID, Product> productsById = new HashMap<>();
        collectCreateRequestedProducts(req.items(), productsById);
        BigDecimal subtotal = attachCreateItems(order, req.items(), productsById);

        BusinessConfig config = businessConfigService.getOrInitConfig();
        assertMinPacksPerLineCreate(config.getMinPacksPerLine(), req.items(), productsById);

        BigDecimal total = subtotal.add(order.getDeliveryCost());
        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTotal(total.setScale(2, RoundingMode.HALF_UP));

        // No se decrementa stock en mayorista.
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    /**
     * Pedido contra el excedente en depósito.
     * <p>{@code deliveryDate} debe ser nulo (la entrega es inmediata). Valida stock disponible
     * y lo descuenta al confirmar.
     */
    public OrderResponse createStock(UUID userId, CreateOrderRequest req) {
        validateItemsPresent(req);
        if (req.deliveryDate() != null) {
            throw new OrderNotEditableException(
                    "Pedido de stock no lleva fecha de entrega (dejá deliveryDate en null).");
        }

        DeliveryMethod dm = deliveryMethodRepository.findByIdAndActiveTrue(req.deliveryMethodId())
                .orElseThrow(() -> new DeliveryMethodNotFoundException(req.deliveryMethodId()));

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDIENTE)
                .type(OrderType.STOCK)
                .deliveryMethodId(dm.getId())
                .deliveryMethodName(dm.getName())
                .deliveryCost(dm.getCost())
                .deliveryAddress(req.deliveryAddress())
                .deliveryPhone(req.deliveryPhone())
                .notes(req.notes())
                .deliveryDate(null)
                .stockDecremented(Boolean.FALSE)
                .build();

        Map<UUID, Product> productsById = new HashMap<>();
        Map<UUID, Integer> requestedByProduct =
                collectCreateRequestedProducts(req.items(), productsById);

        List<InsufficientStockException.InsufficientStockItem> stockErrors = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : requestedByProduct.entrySet()) {
            Product p = productsById.get(entry.getKey());
            int available = p.getStock() != null ? p.getStock() : 0;
            if (entry.getValue() > available) {
                stockErrors.add(new InsufficientStockException.InsufficientStockItem(
                        p.getId(), p.getName(), entry.getValue(), available));
            }
        }
        if (!stockErrors.isEmpty()) {
            throw new InsufficientStockException(stockErrors);
        }

        BigDecimal subtotal = attachCreateItems(order, req.items(), productsById);

        BusinessConfig config = businessConfigService.getOrInitConfig();
        assertMinPacksPerLineCreate(config.getMinPacksPerLine(), req.items(), productsById);

        BigDecimal total = subtotal.add(order.getDeliveryCost());
        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTotal(total.setScale(2, RoundingMode.HALF_UP));

        decrementStockForOrder(order);
        order.setStockDecremented(Boolean.TRUE);

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    // ── Update ───────────────────────────────────────────────────────────

    public OrderResponse updateMine(UUID userId, UUID orderId, UpdateOrderRequest req) {
        Order order = loadOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException(orderId);
        }
        ensureEditable(order);

        if (order.getType() == OrderType.WHOLESALE) {
            return updateWholesale(order, req);
        }
        return updateStock(order, req);
    }

    private OrderResponse updateWholesale(Order order, UpdateOrderRequest req) {
        if (req.deliveryDate() != null) {
            assertDeliveryDateInWindow(req.deliveryDate());
        }

        DeliveryMethod dm = null;
        if (req.deliveryMethodId() != null) {
            dm = deliveryMethodRepository.findByIdAndActiveTrue(req.deliveryMethodId())
                    .orElseThrow(() -> new DeliveryMethodNotFoundException(req.deliveryMethodId()));
        }

        Map<UUID, Product> productsById = new HashMap<>();
        collectUpdateRequestedProducts(req.items(), productsById);
        order.getItems().clear();

        BusinessConfig config = businessConfigService.getOrInitConfig();
        assertMinPacksPerLineUpdate(config.getMinPacksPerLine(), req.items(), productsById);

        BigDecimal subtotal = attachUpdateItems(order, req.items(), productsById);

        if (req.deliveryDate() != null) {
            order.setDeliveryDate(req.deliveryDate());
        }
        if (dm != null) {
            order.setDeliveryMethodId(dm.getId());
            order.setDeliveryMethodName(dm.getName());
            order.setDeliveryCost(dm.getCost());
        }
        if (req.deliveryAddress() != null) order.setDeliveryAddress(req.deliveryAddress());
        if (req.deliveryPhone() != null) order.setDeliveryPhone(req.deliveryPhone());
        if (req.notes() != null) order.setNotes(req.notes());

        BigDecimal total = subtotal.add(order.getDeliveryCost());
        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTotal(total.setScale(2, RoundingMode.HALF_UP));

        // No se toca stock en mayorista.
        return toResponse(order);
    }

    private OrderResponse updateStock(Order order, UpdateOrderRequest req) {
        if (req.deliveryDate() != null) {
            throw new OrderNotEditableException(
                    "Pedido de stock no lleva fecha de entrega (dejá deliveryDate en null).");
        }

        DeliveryMethod dm = null;
        if (req.deliveryMethodId() != null) {
            dm = deliveryMethodRepository.findByIdAndActiveTrue(req.deliveryMethodId())
                    .orElseThrow(() -> new DeliveryMethodNotFoundException(req.deliveryMethodId()));
        }

        if (Boolean.TRUE.equals(order.getStockDecremented())) {
            restoreStockForOrder(order);
            order.setStockDecremented(Boolean.FALSE);
        }

        Map<UUID, Integer> originalItemsByProduct = new HashMap<>();
        for (OrderItem existing : order.getItems()) {
            originalItemsByProduct.merge(existing.getProductId(),
                    existing.getQuantity(), Integer::sum);
        }
        order.getItems().clear();

        Map<UUID, Product> productsById = new HashMap<>();
        Map<UUID, Integer> requestedByProduct =
                collectUpdateRequestedProducts(req.items(), productsById);

        BusinessConfig config = businessConfigService.getOrInitConfig();
        assertMinPacksPerLineUpdate(config.getMinPacksPerLine(), req.items(), productsById);

        List<InsufficientStockException.InsufficientStockItem> stockErrors = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : requestedByProduct.entrySet()) {
            int alreadyOnOrder = originalItemsByProduct.getOrDefault(entry.getKey(), 0);
            Product p = productsById.get(entry.getKey());
            int available = p.getStock() != null ? p.getStock() : 0;
            int netRequested = entry.getValue() - alreadyOnOrder;
            if (netRequested > available) {
                stockErrors.add(new InsufficientStockException.InsufficientStockItem(
                        p.getId(), p.getName(), entry.getValue(), available + alreadyOnOrder));
            }
        }
        if (!stockErrors.isEmpty()) {
            throw new InsufficientStockException(stockErrors);
        }

        BigDecimal subtotal = attachUpdateItems(order, req.items(), productsById);

        if (dm != null) {
            order.setDeliveryMethodId(dm.getId());
            order.setDeliveryMethodName(dm.getName());
            order.setDeliveryCost(dm.getCost());
        }
        order.setDeliveryDate(null);
        if (req.deliveryAddress() != null) order.setDeliveryAddress(req.deliveryAddress());
        if (req.deliveryPhone() != null) order.setDeliveryPhone(req.deliveryPhone());
        if (req.notes() != null) order.setNotes(req.notes());

        BigDecimal total = subtotal.add(order.getDeliveryCost());
        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTotal(total.setScale(2, RoundingMode.HALF_UP));

        decrementStockForOrder(order);
        order.setStockDecremented(Boolean.TRUE);

        return toResponse(order);
    }

    // ── Cancel + transitions ────────────────────────────────────────────

    public OrderResponse cancelMine(UUID userId, UUID orderId) {
        Order order = loadOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException(orderId);
        }
        ensureEditable(order);
        return transitionInternal(order, OrderStatus.CANCELADO, null);
    }

    public OrderResponse transitionStatus(UUID orderId, UpdateOrderStatusRequest req) {
        Order order = loadOrThrow(orderId);
        return transitionInternal(order, req.targetStatus(), req.notes());
    }

    private OrderResponse transitionInternal(Order order, OrderStatus target, String extraNotes) {
        OrderStatus current = order.getStatus();
        boolean isRetiro = order.getDeliveryMethodName() != null
                && order.getDeliveryMethodName().toLowerCase().contains("retiro");
        boolean validTransition = false;

        if (isRetiro) {
            validTransition = switch (current) {
                case PENDIENTE -> target == OrderStatus.ARMADO || target == OrderStatus.CANCELADO;
                case ARMADO -> target == OrderStatus.ENTREGADO || target == OrderStatus.CANCELADO;
                case ENVIADO, ENTREGADO, CANCELADO -> false;
            };
        } else {
            validTransition = current.canTransitionTo(target);
        }

        if (!validTransition) {
            throw new OrderInvalidTransitionException(current, target);
        }

        boolean stockWasDecremented = Boolean.TRUE.equals(order.getStockDecremented());
        if (order.getType() == OrderType.STOCK) {
            if (target == OrderStatus.ARMADO && !stockWasDecremented) {
                decrementStockForOrder(order);
                order.setStockDecremented(Boolean.TRUE);
            }
            if (target == OrderStatus.CANCELADO && stockWasDecremented) {
                restoreStockForOrder(order);
                order.setStockDecremented(Boolean.FALSE);
            }
        }

        if (target.isTerminal()) {
            order.setClosedAt(Instant.now());
        }
        if (extraNotes != null && !extraNotes.isBlank()) {
            String existing = order.getNotes();
            order.setNotes(existing == null || existing.isBlank()
                    ? extraNotes
                    : existing + "\n— " + extraNotes);
        }
        order.setStatus(target);
        return toResponse(order);
    }

    // ── Delivery date override (distribuidor) ──────────────────────────

    public OrderResponse updateDeliveryDate(UUID orderId, LocalDate deliveryDate) {
        Order order = loadOrThrow(orderId);
        if (order.getType() != OrderType.WHOLESALE) {
            throw new OrderNotEditableException(
                    "Solo los pedidos mayoristas tienen fecha de entrega a asignar.");
        }
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new OrderNotEditableException(
                    "La fecha de entrega solo se puede modificar cuando el pedido está PENDIENTE.");
        }
        if (deliveryDate != null) {
            assertDeliveryDateInWindow(deliveryDate);
        }
        order.setDeliveryDate(deliveryDate);
        return toResponse(order);
    }

    // ── Stock helpers ──────────────────────────────────────────────────

    private void decrementStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product p = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));
            int newStock = p.getStock() - item.getQuantity();
            if (newStock < 0) {
                throw new OrderNotEditableException(
                        "Stock insuficiente para " + p.getName() + " (disponible: "
                                + p.getStock() + ", requerido: " + item.getQuantity() + ")");
            }
            p.setStock(newStock);
        }
    }

    private void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(p ->
                    p.setStock(p.getStock() + item.getQuantity()));
        }
    }

    // ── Editability ────────────────────────────────────────────────────

    private void ensureEditable(Order order) {
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new OrderNotEditableException(
                    "El pedido ya no se puede modificar (estado actual: "
                            + order.getStatus() + ").");
        }
        if (order.getType() == OrderType.WHOLESALE) {
            if (order.getDeliveryDate() == null) {
                throw new OrderNotEditableException(
                        "El pedido mayorista no tiene fecha de entrega asignada.");
            }
            if (!deliveryScheduleService.isWithinWindow(order.getDeliveryDate())) {
                throw new DeliveryWindowExpiredException(order.getDeliveryDate());
            }
        }
    }

    private void assertDeliveryDateInWindow(LocalDate deliveryDate) {
        if (!deliveryScheduleService.isWithinWindow(deliveryDate)) {
            throw new DeliveryWindowExpiredException(deliveryDate);
        }
    }

    // ── Item helpers (CREATE) ──────────────────────────────────────────

    private BigDecimal attachCreateItems(Order order,
                                         List<CreateOrderRequest.OrderItemRequest> items,
                                         Map<UUID, Product> productsById) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemRequest itemReq : items) {
            Product p = productsById.get(itemReq.productId());
            int unitsPerPack = p.getUnitsPerPack() != null && p.getUnitsPerPack() >= 1
                    ? p.getUnitsPerPack() : 1;
            int packs = itemReq.quantity();
            int physicalUnits = packs * unitsPerPack;
            BigDecimal itemSubtotal = p.getPrice().multiply(BigDecimal.valueOf(packs));

            OrderItem oi = OrderItem.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .productImageUrl(p.getImageUrl())
                    .packsRequested(packs)
                    .unitsPerPackAtOrder(unitsPerPack)
                    .quantity(physicalUnits)
                    .unitPrice(p.getPrice())
                    .subtotal(itemSubtotal)
                    .build();
            order.addItem(oi);
            subtotal = subtotal.add(itemSubtotal);
        }
        return subtotal;
    }

    private Map<UUID, Integer> collectCreateRequestedProducts(
            List<CreateOrderRequest.OrderItemRequest> items,
            Map<UUID, Product> productsById) {
        Map<UUID, Integer> requested = new HashMap<>();
        for (CreateOrderRequest.OrderItemRequest itemReq : items) {
            Product p = productRepository.findByIdAndActiveTrue(itemReq.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemReq.productId()));
            int unitsPerPack = p.getUnitsPerPack() != null && p.getUnitsPerPack() >= 1
                    ? p.getUnitsPerPack() : 1;
            int packs = itemReq.quantity();
            int physicalUnits = packs * unitsPerPack;
            requested.merge(p.getId(), physicalUnits, Integer::sum);
            productsById.put(p.getId(), p);
        }
        return requested;
    }

    private static void validateItemsPresent(CreateOrderRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new OrderNotEditableException("El pedido no tiene ítems.");
        }
    }

    // ── Item helpers (UPDATE) ──────────────────────────────────────────

    private BigDecimal attachUpdateItems(Order order,
                                         List<UpdateOrderRequest.OrderItemRequest> items,
                                         Map<UUID, Product> productsById) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (UpdateOrderRequest.OrderItemRequest itemReq : items) {
            Product p = productsById.get(itemReq.productId());
            int unitsPerPack = p.getUnitsPerPack() != null && p.getUnitsPerPack() >= 1
                    ? p.getUnitsPerPack() : 1;
            int packs = itemReq.quantity();
            int physicalUnits = packs * unitsPerPack;
            BigDecimal itemSubtotal = p.getPrice().multiply(BigDecimal.valueOf(packs));

            OrderItem oi = OrderItem.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .productImageUrl(p.getImageUrl())
                    .packsRequested(packs)
                    .unitsPerPackAtOrder(unitsPerPack)
                    .quantity(physicalUnits)
                    .unitPrice(p.getPrice())
                    .subtotal(itemSubtotal)
                    .build();
            order.addItem(oi);
            subtotal = subtotal.add(itemSubtotal);
        }
        return subtotal;
    }

    private Map<UUID, Integer> collectUpdateRequestedProducts(
            List<UpdateOrderRequest.OrderItemRequest> items,
            Map<UUID, Product> productsById) {
        Map<UUID, Integer> requested = new HashMap<>();
        for (UpdateOrderRequest.OrderItemRequest itemReq : items) {
            Product p = productRepository.findByIdAndActiveTrue(itemReq.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemReq.productId()));
            int unitsPerPack = p.getUnitsPerPack() != null && p.getUnitsPerPack() >= 1
                    ? p.getUnitsPerPack() : 1;
            int packs = itemReq.quantity();
            int physicalUnits = packs * unitsPerPack;
            requested.merge(p.getId(), physicalUnits, Integer::sum);
            productsById.put(p.getId(), p);
        }
        return requested;
    }

    /**
     * Verifica que cada línea del pedido alcance el mínimo de packs por línea
     * configurado en {@code BusinessConfig.minPacksPerLine}. Si alguna no llega,
     * lanza {@link MinPacksPerLineException} con el detalle de las que fallan.
     */
    private void assertMinPacksPerLineCreate(int min,
                                             List<CreateOrderRequest.OrderItemRequest> items,
                                             Map<UUID, Product> productsById) {
        List<MinPacksPerLineException.OffendingLine> bad = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest itemReq : items) {
            if (itemReq.quantity() < min) {
                Product p = productsById.get(itemReq.productId());
                String name = p != null ? p.getName() : itemReq.productId().toString();
                bad.add(new MinPacksPerLineException.OffendingLine(
                        itemReq.productId(), name, itemReq.quantity(), min));
            }
        }
        if (!bad.isEmpty()) throw new MinPacksPerLineException(bad, min);
    }

    private void assertMinPacksPerLineUpdate(int min,
                                             List<UpdateOrderRequest.OrderItemRequest> items,
                                             Map<UUID, Product> productsById) {
        List<MinPacksPerLineException.OffendingLine> bad = new ArrayList<>();
        for (UpdateOrderRequest.OrderItemRequest itemReq : items) {
            if (itemReq.quantity() < min) {
                Product p = productsById.get(itemReq.productId());
                String name = p != null ? p.getName() : itemReq.productId().toString();
                bad.add(new MinPacksPerLineException.OffendingLine(
                        itemReq.productId(), name, itemReq.quantity(), min));
            }
        }
        if (!bad.isEmpty()) throw new MinPacksPerLineException(bad, min);
    }

    // ── Misc ───────────────────────────────────────────────────────────

    private Order loadOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private OrderResponse toResponse(Order order) {
        User customer = userRepository.findById(order.getUserId()).orElse(null);
        String customerName = customer != null
                ? customer.getFirstName() + " " + customer.getLastName()
                : "—";
        String customerEmail = customer != null ? customer.getEmail() : null;

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getId(),
                        i.getProductId(),
                        i.getProductName(),
                        i.getProductImageUrl(),
                        i.getQuantity(),
                        i.getPacksRequested(),
                        i.getUnitsPerPackAtOrder(),
                        i.getUnitPrice(),
                        i.getSubtotal()))
                .toList();

        boolean editable = order.getStatus() == OrderStatus.PENDIENTE
                && isEditableByTypeAndDate(order);

        LocalDate dateValue = order.getDeliveryDate();
        if (order.getDeliveryMethodName() != null
                && order.getDeliveryMethodName().toLowerCase().contains("retiro")) {
            dateValue = null;
        }
        if (order.getType() == OrderType.STOCK) {
            dateValue = null;
        }

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                customerName,
                customerEmail,
                order.getStatus(),
                order.getType(),
                order.getDeliveryMethodId(),
                order.getDeliveryMethodName(),
                order.getDeliveryCost(),
                order.getSubtotal(),
                order.getTotal(),
                order.getDeliveryAddress(),
                order.getDeliveryPhone(),
                order.getNotes(),
                dateValue,
                editable,
                order.getItems().size(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getClosedAt()
        );
    }

    private boolean isEditableByTypeAndDate(Order order) {
        if (order.getType() == OrderType.WHOLESALE) {
            return order.getDeliveryDate() != null
                    && deliveryScheduleService.isWithinWindow(order.getDeliveryDate());
        }
        return order.getStatus() == OrderStatus.PENDIENTE;
    }
}
