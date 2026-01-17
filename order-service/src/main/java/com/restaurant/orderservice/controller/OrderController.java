package com.restaurant.orderservice.controller;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.orderservice.dto.*;
import com.restaurant.securitymodule.model.UserPrincipal;
import com.restaurant.orderservice.service.OrderService;
import com.restaurant.redismodule.exception.CacheException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ========== CUSTOMER ENDPOINTS ==========

    /**
     * Create new order (authenticated user)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) throws DataFactoryException {

        String userId = extractUserId(authentication);
        log.info("POST /api/orders - Creating order for user: {}", userId);

        OrderDto order = orderService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Create guest order (no authentication required)
     */
    @PostMapping("/guest")
    public ResponseEntity<OrderDto> createGuestOrder(
            @Valid @RequestBody CreateOrderRequest request) throws DataFactoryException {

        log.info("POST /api/orders/guest - Creating guest order");
        OrderDto order = orderService.createGuestOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> getOrderById(
            @PathVariable Long id) throws CacheException, DataFactoryException {

        log.info("GET /api/orders/{}", id);
        OrderDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Get current user's orders
     */
    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderDto>> getMyOrders(
            Authentication authentication) throws CacheException, DataFactoryException {

        String userId = extractUserId(authentication);
        log.info("GET /api/orders/my-orders - user: {}", userId);

        List<OrderDto> orders = orderService.getMyOrders(userId, null);
        return ResponseEntity.ok(orders);
    }

    /**
     * Update order (before confirmed)
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request,
            Authentication authentication) throws CacheException, DataFactoryException {

        String userId = extractUserId(authentication);
        log.info("PUT /api/orders/{} - user: {}", id, userId);

        OrderDto order = orderService.updateOrder(id, request, userId);
        return ResponseEntity.ok(order);
    }

    /**
     * Cancel order
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) throws CacheException, DataFactoryException {

        String userId = extractUserId(authentication);
        log.info("DELETE /api/orders/{} - user: {}", id, userId);

        orderService.cancelOrder(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ========== ADMIN ENDPOINTS ==========

    /**
     * Get all orders (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderDto>> getAllOrders() throws CacheException, DataFactoryException {

        log.info("GET /api/orders - Admin access");
        List<OrderDto> orders = orderService.getAllOrders(null);
        return ResponseEntity.ok(orders);
    }

    /**
     * Update order status (Admin/Staff)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN')")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) throws CacheException, DataFactoryException {

        log.info("PATCH /api/orders/{}/status - new status: {}", id, request.getNewStatus());
        OrderDto order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(order);
    }

    /**
     * Assign driver to order (Admin only)
     */
    @PatchMapping("/{id}/assign-driver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDto> assignDriver(
            @PathVariable Long id,
            @Valid @RequestBody AssignDriverRequest request) throws CacheException, DataFactoryException {

        log.info("PATCH /api/orders/{}/assign-driver - driver: {}", id, request.getDriverId());
        OrderDto order = orderService.assignDriver(id, request);
        return ResponseEntity.ok(order);
    }

    /**
     * Get kitchen queue
     */
    @GetMapping("/kitchen-queue")
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN')")
    public ResponseEntity<List<OrderDto>> getKitchenQueue() throws CacheException, DataFactoryException {

        log.info("GET /api/orders/kitchen-queue");
        List<OrderDto> orders = orderService.getKitchenQueue();
        return ResponseEntity.ok(orders);
    }

    /**
     * Admin: Create dine-in order (for walk-in customers)
     */
    @PostMapping("/admin/dine-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN')")
    public ResponseEntity<OrderDto> createDineInOrder(
            @Valid @RequestBody AdminCreateDineInRequest request) throws DataFactoryException {

        log.info("POST /api/orders/admin/dine-in - Creating dine-in order for table: {}", request.getTableId());
        OrderDto order = orderService.createDineInOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Admin: Update payment status
     */
    @PatchMapping("/{id}/payment-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDto> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentStatusRequest request) throws CacheException, DataFactoryException {

        log.info("PATCH /api/orders/{}/payment-status - new status: {}", id, request.getNewPaymentStatus());
        OrderDto order = orderService.updatePaymentStatus(id, request);
        return ResponseEntity.ok(order);
    }

    // ========== DRIVER ENDPOINTS ==========

    /**
     * Get orders assigned to current driver
     */
    @GetMapping("/driver/assigned")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<OrderDto>> getDriverAssignedOrders(
            Authentication authentication) throws CacheException, DataFactoryException {

        String driverId = extractUserId(authentication);
        log.info("GET /api/orders/driver/assigned - driver: {}", driverId);

        List<OrderDto> orders = orderService.getDriverAssignedOrders(driverId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Mark order as out for delivery
     */
    @PatchMapping("/{id}/out-for-delivery")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<OrderDto> markOutForDelivery(
            @PathVariable Long id,
            Authentication authentication) throws CacheException, DataFactoryException {

        String driverId = extractUserId(authentication);
        log.info("PATCH /api/orders/{}/out-for-delivery - driver: {}", id, driverId);

        OrderDto order = orderService.markOutForDelivery(id, driverId);
        return ResponseEntity.ok(order);
    }

    /**
     * Mark order as delivered
     */
    @PatchMapping("/{id}/delivered")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<OrderDto> markDelivered(
            @PathVariable Long id,
            Authentication authentication) throws CacheException, DataFactoryException {

        String driverId = extractUserId(authentication);
        log.info("PATCH /api/orders/{}/delivered - driver: {}", id, driverId);

        OrderDto order = orderService.markDelivered(id, driverId);
        return ResponseEntity.ok(order);
    }

    // ========== PRE-ORDER ENDPOINT ==========

    /**
     * Create pre-order for reservation
     */
    @PostMapping("/pre-order/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> createPreOrder(
            @PathVariable Long reservationId,
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) throws DataFactoryException {

        String userId = extractUserId(authentication);
        log.info("POST /api/orders/pre-order/{} - user: {}", reservationId, userId);

        OrderDto order = orderService.createPreOrder(reservationId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Create guest pre-order for reservation (no authentication required)
     */
    @PostMapping("/guest/pre-order/{reservationId}")
    public ResponseEntity<OrderDto> createGuestPreOrder(
            @PathVariable Long reservationId,
            @Valid @RequestBody CreateOrderRequest request) throws DataFactoryException {

        log.info("POST /api/orders/guest/pre-order/{} - Creating guest pre-order", reservationId);
        OrderDto order = orderService.createGuestPreOrder(reservationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // ========== HELPER METHODS ==========

    private String extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return principal.getUserId();
        }
        throw new IllegalStateException("User ID not found in authentication");
    }
}