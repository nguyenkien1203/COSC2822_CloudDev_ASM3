package com.restaurant.orderservice.service.impl;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.orderservice.dto.*;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.enums.OrderType;
import com.restaurant.orderservice.enums.PaymentStatus;
import com.restaurant.orderservice.exception.MenuItemNotFoundException;
import com.restaurant.orderservice.exception.MenuServiceException;
import com.restaurant.orderservice.exception.UnauthorizedOrderAccessException;
import com.restaurant.orderservice.factory.OrderFactory;
import com.restaurant.orderservice.filter.OrderFilter;
import com.restaurant.orderservice.service.MenuServiceClient;
import com.restaurant.orderservice.service.OrderProducerService;
import com.restaurant.orderservice.service.OrderService;
import com.restaurant.redismodule.exception.CacheException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // Tax and fee constants
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08"); // 8%
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("5.00"); // $5.00

    private final OrderFactory orderFactory;
    private final MenuServiceClient menuServiceClient;

    @Autowired
    private final OrderProducerService orderProducerService;

    @Override
    public OrderDto createOrder(CreateOrderRequest request, String userId) throws DataFactoryException {
        log.info("Create order with orderId");

        validateOrderRequest(request);

        List<OrderItemDto> orderItems = buildOrderItems(request.getItems());

        // Calculate amounts
        BigDecimal subtotal = calculateSubtotal(orderItems);
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal deliveryFee = request.getOrderType() == OrderType.DELIVERY ? DELIVERY_FEE : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(taxAmount).add(deliveryFee);

        OrderDto orderDto = OrderDto.builder()
                .userId(userId)
                .orderType(request.getOrderType())
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .deliveryFee(deliveryFee)
                .totalAmount(totalAmount)
                .deliveryAddress(request.getDeliveryAddress())
                .reservationId(request.getReservationId())
                .notes(request.getNotes())
                .estimatedPickupTime(request.getEstimatedPickupTime())
                .orderItems(orderItems)
                .build();

        OrderDto createdOrder = orderFactory.create(orderDto);
        orderProducerService.publishOrderCreatedEvent(createdOrder);
        return createdOrder;
    }

    @Override
    public OrderDto createGuestOrder(CreateOrderRequest request) throws DataFactoryException {
        log.info("Create guest order");

        validateOrderRequest(request);

        List<OrderItemDto> orderItems = buildOrderItems(request.getItems());

        // Calculate amounts
        BigDecimal subtotal = calculateSubtotal(orderItems);
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal deliveryFee = request.getOrderType() == OrderType.DELIVERY ? DELIVERY_FEE : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(taxAmount).add(deliveryFee);

        OrderDto orderDto = OrderDto.builder()
                .guestEmail(request.getGuestEmail())
                .guestPhone(request.getGuestPhone())
                .guestName(request.getGuestName())
                .orderType(request.getOrderType())
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .deliveryFee(deliveryFee)
                .totalAmount(totalAmount)
                .deliveryAddress(request.getDeliveryAddress())
                .notes(request.getNotes())
                .estimatedPickupTime(request.getEstimatedPickupTime())
                .orderItems(orderItems)
                .build();

        OrderDto createdOrder = orderFactory.create(orderDto);
        orderProducerService.publishOrderCreatedEvent(createdOrder);
        return createdOrder;
    }

    @Override
    @Transactional
    public OrderDto getOrderById(Long id) throws CacheException, DataFactoryException {
        log.info("Getting order by id: {}", id);
        return orderFactory.getModel(id, null);
    }

    @Override
    @Transactional
    public List<OrderDto> getMyOrders(String userId, OrderFilter filter) throws CacheException, DataFactoryException {
        log.info("Getting orders for user: {}", userId);

        OrderFilter userFilter = OrderFilter.builder()
                .userId(userId)
                .build();

        return orderFactory.getList(userFilter);
    }

    @Override
    @Transactional
    public OrderDto updateOrder(Long id, UpdateOrderRequest request, String userId)
            throws CacheException, DataFactoryException {
        log.info("Updating order: {} by user: {}", id, userId);

        OrderDto existingOrder = orderFactory.getModel(id, null);

        // Validate ownership
        if (!existingOrder.getUserId().equals(userId)) {
            throw new UnauthorizedOrderAccessException("You don't have permission to update this order");
        }

        // Can only update PENDING orders
        if (existingOrder.getStatus() != OrderStatus.PENDING) {
            throw new DataFactoryException("Cannot update order that is already " + existingOrder.getStatus());
        }

        // Update fields
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<OrderItemDto> newItems = buildOrderItems(request.getItems());
            existingOrder.setOrderItems(newItems);

            // Recalculate amounts
            BigDecimal subtotal = calculateSubtotal(newItems);
            BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal deliveryFee = existingOrder.getOrderType() == OrderType.DELIVERY ? DELIVERY_FEE
                    : BigDecimal.ZERO;
            BigDecimal totalAmount = subtotal.add(taxAmount).add(deliveryFee);

            existingOrder.setSubtotal(subtotal);
            existingOrder.setTaxAmount(taxAmount);
            existingOrder.setDeliveryFee(deliveryFee);
            existingOrder.setTotalAmount(totalAmount);
        }
        if (request.getPaymentMethod() != null) {
            existingOrder.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getDeliveryAddress() != null) {
            existingOrder.setDeliveryAddress(request.getDeliveryAddress());
        }
        if (request.getNotes() != null) {
            existingOrder.setNotes(request.getNotes());
        }

        return orderFactory.update(existingOrder, null);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id, String userId) throws CacheException, DataFactoryException {
        log.info("Cancelling order: {} by user: {}", id, userId);

        OrderDto order = orderFactory.getModel(id, null);

        // Validate ownership
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedOrderAccessException("You don't have permission to cancel this order");
        }

        // Can only cancel PENDING or CONFIRMED orders
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new DataFactoryException("Cannot cancel order that is already " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderFactory.update(order, null);

        orderProducerService.publishOrderCancelledEvent(order, "Cancelled by customer");
    }

    @Override
    @Transactional
    public List<OrderDto> getAllOrders(OrderFilter filter) throws CacheException, DataFactoryException {
        log.info("Getting all orders with filter");
        return orderFactory.getList(filter);
    }

    @Override
    public OrderDto updateOrderStatus(Long id, UpdateOrderStatusRequest request)
            throws CacheException, DataFactoryException {
        log.info("Updating order status: {} to {}", id, request.getNewStatus());

        OrderDto order = orderFactory.getModel(id, null);
        OrderStatus oldStatus = order.getStatus();

        validateStatusTransition(oldStatus, request.getNewStatus());

        order.setStatus(request.getNewStatus());

        // Set estimated ready time when confirmed
        if (request.getNewStatus() == OrderStatus.CONFIRMED) {
            order.setEstimatedReadyTime(LocalDateTime.now().plusMinutes(30)); // Default 30 mins
        }

        OrderDto updatedOrder = orderFactory.update(order, null);
        orderProducerService.publishOrderStatusChangedEvent(updatedOrder, oldStatus, request.getNewStatus());

        return updatedOrder;
    }

    @Override
    public OrderDto assignDriver(Long orderId, AssignDriverRequest request)
            throws CacheException, DataFactoryException {
        log.info("Assigning driver {} to order {}", request.getDriverId(), orderId);

        OrderDto order = orderFactory.getModel(orderId, null);

        if (order.getOrderType() != OrderType.DELIVERY) {
            throw new DataFactoryException("Can only assign driver to delivery orders");
        }

        if (order.getStatus() != OrderStatus.READY) {
            throw new DataFactoryException("Can only assign driver to orders that are READY");
        }

        order.setDriverId(request.getDriverId());
        OrderDto updatedOrder = orderFactory.update(order, null);

        orderProducerService.publishDeliveryAssignedEvent(updatedOrder);

        return updatedOrder;
    }

    @Override
    public List<OrderDto> getKitchenQueue() throws CacheException, DataFactoryException {
        log.info("Getting kitchen queue");
        return orderFactory.getKitchenQueue();
    }

    @Override
    @Transactional
    public OrderDto createDineInOrder(AdminCreateDineInRequest request) throws DataFactoryException {
        log.info("Admin creating dine-in order for table: {}", request.getTableId());

        List<OrderItemDto> orderItems = buildOrderItems(request.getItems());

        // Calculate amounts
        BigDecimal subtotal = calculateSubtotal(orderItems);
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount); // No delivery fee for dine-in

        OrderDto orderDto = OrderDto.builder()
                .orderType(OrderType.DINE_IN)
                .status(OrderStatus.CONFIRMED) // Auto-confirm since customer is present
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .deliveryFee(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .reservationId(request.getReservationId())
                .notes(request.getNotes())
                .estimatedReadyTime(LocalDateTime.now().plusMinutes(20)) // Faster for dine-in
                .orderItems(orderItems)
                .build();

        OrderDto createdOrder = orderFactory.create(orderDto);
        orderProducerService.publishOrderCreatedEvent(createdOrder);

        log.info("Admin created dine-in order: {}", createdOrder.getId());
        return createdOrder;
    }

    @Override
    @Transactional
    public OrderDto updatePaymentStatus(Long orderId, UpdatePaymentStatusRequest request)
            throws CacheException, DataFactoryException {
        log.info("Admin updating payment status for order {} to {}", orderId, request.getNewPaymentStatus());

        OrderDto order = orderFactory.getModel(orderId, null);
        PaymentStatus oldStatus = order.getPaymentStatus();

        order.setPaymentStatus(request.getNewPaymentStatus());
        OrderDto updatedOrder = orderFactory.update(order, null);

        log.info("Payment status updated from {} to {} for order {}",
                oldStatus, request.getNewPaymentStatus(), orderId);
        return updatedOrder;
    }

    @Override
    public List<OrderDto> getDriverAssignedOrders(String driverId) throws CacheException, DataFactoryException {
        log.info("Getting orders assigned to driver: {}", driverId);

        OrderFilter filter = OrderFilter.builder()
                .driverId(driverId)
                .build();

        return orderFactory.getList(filter);
    }

    @Override
    public OrderDto markOutForDelivery(Long orderId, String driverId) throws CacheException, DataFactoryException {
        log.info("Marking order {} as out for delivery by driver {}", orderId, driverId);

        OrderDto order = orderFactory.getModel(orderId, null);

        // Validate driver assignment
        if (!driverId.equals(order.getDriverId())) {
            throw new UnauthorizedOrderAccessException("This order is not assigned to you");
        }

        if (order.getStatus() != OrderStatus.READY) {
            throw new DataFactoryException("Order must be READY to mark as out for delivery");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        OrderDto updatedOrder = orderFactory.update(order, null);

        orderProducerService.publishOrderStatusChangedEvent(updatedOrder, oldStatus, OrderStatus.OUT_FOR_DELIVERY);

        return updatedOrder;
    }

    @Override
    public OrderDto markDelivered(Long orderId, String driverId) throws CacheException, DataFactoryException {
        log.info("Marking order {} as delivered by driver {}", orderId, driverId);

        OrderDto order = orderFactory.getModel(orderId, null);

        // Validate driver assignment
        if (!driverId.equals(order.getDriverId())) {
            throw new UnauthorizedOrderAccessException("This order is not assigned to you");
        }

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new DataFactoryException("Order must be OUT_FOR_DELIVERY to mark as delivered");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.DELIVERED);
        order.setActualDeliveryTime(LocalDateTime.now());
        OrderDto updatedOrder = orderFactory.update(order, null);

        orderProducerService.publishOrderStatusChangedEvent(updatedOrder, oldStatus, OrderStatus.DELIVERED);
        orderProducerService.publishDeliveryCompletedEvent(updatedOrder);

        return updatedOrder;
    }

    @Override
    public OrderDto createPreOrder(Long reservationId, CreateOrderRequest request, String userId)
            throws DataFactoryException {
        log.info("Creating pre-order for reservation: {} by user: {}", reservationId, userId);

        request.setOrderType(OrderType.PRE_ORDER);
        request.setReservationId(reservationId);

        OrderDto createdOrder = createOrder(request, userId);

        // Publish pre-order created event for reservation service
        orderProducerService.publishPreOrderCreatedEvent(createdOrder);

        return createdOrder;
    }

    @Override
    public OrderDto createGuestPreOrder(Long reservationId, CreateOrderRequest request)
            throws DataFactoryException {
        log.info("Creating guest pre-order for reservation: {}", reservationId);

        request.setOrderType(OrderType.PRE_ORDER);
        request.setReservationId(reservationId);

        OrderDto createdOrder = createGuestOrder(request);

        // Publish pre-order created event for reservation service
        orderProducerService.publishPreOrderCreatedEvent(createdOrder);

        return createdOrder;
    }

    @Override
    @Transactional
    public void confirmPreOrder(Long orderId) throws CacheException, DataFactoryException {
        log.info("Confirming pre-order: {} (triggered by CustomerSeatedEvent)", orderId);

        OrderDto order = orderFactory.getModel(orderId, null);

        // Only confirm if order is in PENDING status
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Pre-order {} is not in PENDING status (current: {}), skipping confirmation",
                    orderId, order.getStatus());
            return;
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setEstimatedReadyTime(LocalDateTime.now().plusMinutes(30));

        OrderDto updatedOrder = orderFactory.update(order, null);
        orderProducerService.publishOrderStatusChangedEvent(updatedOrder, oldStatus, OrderStatus.CONFIRMED);

        log.info("Pre-order {} confirmed successfully", orderId);
    }

    @Override
    @Transactional
    public void cancelOrderByEvent(Long orderId, String reason) throws CacheException, DataFactoryException {
        log.info("Cancelling order: {} with reason: {} (triggered by event)", orderId, reason);

        OrderDto order = orderFactory.getModel(orderId, null);

        // Can only cancel PENDING or CONFIRMED orders
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            log.warn("Order {} cannot be cancelled (current status: {})", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderFactory.update(order, null);

        orderProducerService.publishOrderCancelledEvent(order, reason);
        log.info("Order {} cancelled successfully", orderId);
    }

    /*
     * ==================HELPER METHOD ============================================
     */

    private BigDecimal calculateSubtotal(List<OrderItemDto> orderItems) {
        return orderItems.stream()
                .map(OrderItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<OrderItemDto> buildOrderItems(
            @NotEmpty(message = "Order must have at least one item") @Valid List<CreateOrderItemRequest> items) {

        List<OrderItemDto> orderItems = new ArrayList<>();

        for (CreateOrderItemRequest item : items) {
            try {
                MenuItemDto menuItem = menuServiceClient.getMenuItemById(
                        item.getMenuItemId());
                if (menuItem == null) {
                    throw new MenuItemNotFoundException(item.getMenuItemId());
                }

                if (!Boolean.TRUE.equals(menuItem.getIsAvailable())) {
                    throw new DataFactoryException(
                            "Menu item '" + menuItem.getName() + "' is currently unavailable");
                }
                // Build order item with actual menu data
                BigDecimal subtotal = menuItem.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));

                OrderItemDto orderItem = OrderItemDto.builder()
                        .menuItemId(menuItem.getId())
                        .menuItemName(menuItem.getName())
                        .quantity(item.getQuantity())
                        .unitPrice(menuItem.getPrice())
                        .subtotal(subtotal)
                        .notes(item.getNotes())
                        .build();

                orderItems.add(orderItem);

            } catch (MenuItemNotFoundException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error fetching menu item {}: {}", item.getMenuItemId(), e.getMessage());
                throw new MenuServiceException(
                        "Failed to fetch menu item details for ID: " + item.getMenuItemId(), e);
            }
        }
        return orderItems;
    }

    private void validateOrderRequest(CreateOrderRequest request) throws DataFactoryException {
        // orderType is required for regular orders (pre-orders set it automatically)
        if (request.getOrderType() == null) {
            throw new DataFactoryException("Order type is required");
        }

        if (request.getOrderType() == OrderType.DELIVERY &&
                (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank())) {
            throw new DataFactoryException("Delivery address is required for delivery orders");
        }

        if (request.getOrderType() == OrderType.PRE_ORDER && request.getReservationId() == null) {
            throw new DataFactoryException("Reservation ID is required for pre-orders");
        }

        if (request.getOrderType() == OrderType.TAKEAWAY && request.getEstimatedPickupTime() == null) {
            throw new DataFactoryException("Estimated pickup time is required for takeaway orders");
        }
    }

    private void validateGuestOrderRequest(CreateOrderRequest request) throws DataFactoryException {
        if (request.getGuestEmail() == null || request.getGuestEmail().isBlank()) {
            throw new DataFactoryException("Guest email is required");
        }
        if (request.getGuestPhone() == null || request.getGuestPhone().isBlank()) {
            throw new DataFactoryException("Guest phone is required");
        }
        validateOrderRequest(request);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) throws DataFactoryException {
        // Define valid transitions
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED;
            case PREPARING -> next == OrderStatus.READY || next == OrderStatus.CANCELLED;
            case READY -> next == OrderStatus.OUT_FOR_DELIVERY || next == OrderStatus.COMPLETED;
            case OUT_FOR_DELIVERY -> next == OrderStatus.DELIVERED;
            case DELIVERED, COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new DataFactoryException("Invalid status transition from " + current + " to " + next);
        }
    }
}
