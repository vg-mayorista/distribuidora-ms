package com.distribuidora.service;

import com.distribuidora.dto.order.CreateOrderRequest;
import com.distribuidora.dto.order.OrderItemResponse;
import com.distribuidora.dto.order.OrderResponse;
import com.distribuidora.dto.order.UpdateOrderRequest;
import com.distribuidora.dto.order.UpdateOrderStatusRequest;
import com.distribuidora.exception.DeliveryMethodNotFoundException;
import com.distribuidora.exception.InsufficientStockException;
import com.distribuidora.exception.OrderInvalidTransitionException;
import com.distribuidora.exception.OrderNotEditableException;
import com.distribuidora.exception.OrderNotFoundException;
import com.distribuidora.exception.ProductNotFoundException;
import com.distribuidora.exception.MinOrderRequirementsNotMetException;
import com.distribuidora.model.BusinessConfig;
import com.distribuidora.model.DeliveryMethod;
import com.distribuidora.model.Order;
import com.distribuidora.model.OrderItem;
import com.distribuidora.model.OrderStatus;
import com.distribuidora.model.Product;
import com.distribuidora.model.Role;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DeliveryMethodRepository deliveryMethodRepository;
    private final UserRepository userRepository;
    private final BusinessConfigService businessConfigService;

    private final Clock clock = Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));

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

    public OrderResponse create(UUID userId, CreateOrderRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new OrderNotEditableException("El pedido no tiene ítems.");
        }

        DeliveryMethod dm = deliveryMethodRepository.findByIdAndActiveTrue(req.deliveryMethodId())
                .orElseThrow(() -> new DeliveryMethodNotFoundException(req.deliveryMethodId()));

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDIENTE)
                .deliveryMethodId(dm.getId())
                .deliveryMethodName(dm.getName())
                .deliveryCost(dm.getCost())
                .deliveryAddress(req.deliveryAddress())
                .deliveryPhone(req.deliveryPhone())
                .notes(req.notes())
                .deliveryDate(req.deliveryDate())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<InsufficientStockException.InsufficientStockItem> stockErrors = new ArrayList<>();
        Map<UUID, Integer> requestedByProduct = new HashMap<>();
        Map<UUID, Product> productsById = new HashMap<>();
        for (CreateOrderRequest.OrderItemRequest itemReq : req.items()) {
            Product p = productRepository.findByIdAndActiveTrue(itemReq.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemReq.productId()));
            int unitsPerPack = p.getUnitsPerPack() != null && p.getUnitsPerPack() >= 1 ? p.getUnitsPerPack() : 1;
            int packs = itemReq.quantity();
            int physicalUnits = packs * unitsPerPack;
            requestedByProduct.merge(p.getId(), physicalUnits, Integer::sum);
            productsById.put(p.getId(), p);
        }
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

        for (CreateOrderRequest.OrderItemRequest itemReq : req.items()) {
            Product p = productsById.get(itemReq.productId());
            int unitsPerPack = p.getUnitsPerPack() != null && p.getUnitsPerPack() >= 1 ? p.getUnitsPerPack() : 1;
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

        BusinessConfig config = businessConfigService.getOrInitConfig();
        int totalPacks = req.items().stream().mapToInt(CreateOrderRequest.OrderItemRequest::quantity).sum();
        if (subtotal.compareTo(config.getMinOrderAmount()) < 0 || totalPacks < config.getMinOrderUnits()) {
            throw new MinOrderRequirementsNotMetException(subtotal, config.getMinOrderAmount(), totalPacks, config.getMinOrderUnits());
        }

        BigDecimal total = subtotal.add(order.getDeliveryCost());
        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTotal(total.setScale(2, RoundingMode.HALF_UP));

        decrementStockForOrder(order);
        order.setStockDecremented(Boolean.TRUE);

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    public OrderResponse updateMine(UUID userId, UUID orderId, UpdateOrderRequest req) {
        Order order = loadOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException(orderId);
        }
        ensureEditable(order);

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
            originalItemsByProduct.merge(existing.getProductId(), existing.getQuantity(), Integer::sum);
        }
        order.getItems().clear();
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<UUID, Integer> requestedByProduct = new HashMap<>();
        Map<UUID, Product> productsById = new HashMap<>();
        for (UpdateOrderRequest.OrderItemRequest itemReq : req.items()) {
            Product p = productRepository.findByIdAndActiveTrue(itemReq.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemReq.productId()));
            int unitsPerPack = p.getUnitsPerPack() != null && p.getUnitsPerPack() >= 1 ? p.getUnitsPerPack() : 1;
            int packs = itemReq.quantity();
            int physicalUnits = packs * unitsPerPack;
            requestedByProduct.merge(p.getId(), physicalUnits, Integer::sum);
            productsById.put(p.getId(), p);
        }
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

        for (UpdateOrderRequest.OrderItemRequest itemReq : req.items()) {
            Product p = productsById.get(itemReq.productId());
            int unitsPerPack = p.getUnitsPerPack() != null && p.getUnitsPerPack() >= 1 ? p.getUnitsPerPack() : 1;
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

        if (dm != null) {
            order.setDeliveryMethodId(dm.getId());
            order.setDeliveryMethodName(dm.getName());
            order.setDeliveryCost(dm.getCost());
        }
        order.setDeliveryDate(req.deliveryDate() != null ? req.deliveryDate() : order.getDeliveryDate());
        order.setDeliveryAddress(req.deliveryAddress());
        order.setDeliveryPhone(req.deliveryPhone());
        order.setNotes(req.notes());

        BigDecimal total = subtotal.add(order.getDeliveryCost());
        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTotal(total.setScale(2, RoundingMode.HALF_UP));

        decrementStockForOrder(order);
        order.setStockDecremented(Boolean.TRUE);

        return toResponse(order);
    }

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
        boolean isRetiro = order.getDeliveryMethodName() != null && order.getDeliveryMethodName().toLowerCase().contains("retiro");
        boolean validTransition = false;
        
        if (isRetiro) {
            validTransition = switch (current) {
                case PENDIENTE -> target == OrderStatus.ARMADO || target == OrderStatus.CANCELADO;
                case ARMADO    -> target == OrderStatus.ENTREGADO || target == OrderStatus.CANCELADO;
                case ENVIADO, ENTREGADO, CANCELADO -> false;
            };
        } else {
            validTransition = current.canTransitionTo(target);
        }
        
        if (!validTransition) {
            throw new OrderInvalidTransitionException(current, target);
        }

        if (target == OrderStatus.ARMADO && Boolean.FALSE.equals(order.getStockDecremented())) {
            decrementStockForOrder(order);
            order.setStockDecremented(Boolean.TRUE);
        }
        if (target == OrderStatus.CANCELADO && Boolean.TRUE.equals(order.getStockDecremented())) {
            restoreStockForOrder(order);
            order.setStockDecremented(Boolean.FALSE);
        }

        if (target.isTerminal()) {
            order.setClosedAt(java.time.Instant.now());
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

    private void decrementStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product p = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));
            int newStock = p.getStock() - item.getQuantity();
            if (newStock < 0) {
                throw new OrderNotEditableException(
                        "Stock insuficiente para " + p.getName() + " (disponible: " + p.getStock() + ", requerido: " + item.getQuantity() + ")");
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

    private void ensureEditable(Order order) {
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new OrderNotEditableException(
                    "El pedido ya no se puede modificar (estado actual: " + order.getStatus() + ").");
        }
        if (order.getDeliveryDate() != null && !isEditableByDate(order.getDeliveryDate())) {
            throw new OrderNotEditableException(
                    "El pedido ya no se puede modificar: pasó la fecha de edición.");
        }
    }

    private boolean isEditableByDate(LocalDate deliveryDate) {
        LocalDate today = LocalDate.now(clock);
        return deliveryDate.isAfter(today);
    }

    private Order loadOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public OrderResponse updateDeliveryDate(UUID orderId, LocalDate deliveryDate) {
        Order order = loadOrThrow(orderId);
        if (order.getStatus() != OrderStatus.ARMADO) {
            throw new OrderNotEditableException("La fecha de entrega solo se puede asignar cuando el pedido está armado.");
        }
        if (order.getDeliveryMethodName() != null && order.getDeliveryMethodName().toLowerCase().contains("retiro")) {
            throw new OrderNotEditableException("Un pedido con retiro en local no requiere fecha de envío.");
        }
        order.setDeliveryDate(deliveryDate);
        return toResponse(order);
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
                && (order.getDeliveryDate() == null || isEditableByDate(order.getDeliveryDate()));

        java.time.LocalDate dateValue = order.getDeliveryDate();
        if (order.getDeliveryMethodName() != null && order.getDeliveryMethodName().toLowerCase().contains("retiro")) {
            dateValue = null;
        }

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                customerName,
                customerEmail,
                order.getStatus(),
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
}
