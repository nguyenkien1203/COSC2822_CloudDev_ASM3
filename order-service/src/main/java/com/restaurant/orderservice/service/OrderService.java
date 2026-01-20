package com.restaurant.orderservice.service;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.orderservice.dto.*;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.filter.OrderFilter;
import com.restaurant.redismodule.exception.CacheException;

import java.util.List;

public interface OrderService {

        // ========== CUSTOMER OPERATIONS ==========

        /**
         * Create a new order for registered user
         */
        OrderDto createOrder(CreateOrderRequest request, String userId) throws DataFactoryException;

        /**
         * Create a new order for guest (no account)
         */
        OrderDto createGuestOrder(CreateOrderRequest request) throws DataFactoryException;

        /**
         * Get order by ID
         */
        OrderDto getOrderById(Long id) throws CacheException, DataFactoryException;

        /**
         * Get current user's order history
         */
        List<OrderDto> getMyOrders(String userId, OrderFilter filter) throws CacheException, DataFactoryException;

        /**
         * Update order (before confirmed)
         */
        OrderDto updateOrder(Long id, UpdateOrderRequest request, String userId)
                        throws CacheException, DataFactoryException;

        /**
         * Cancel order (before preparing)
         */
        void cancelOrder(Long id, String userId) throws CacheException, DataFactoryException;

        // ========== ADMIN/STAFF OPERATIONS ==========

        /**
         * Get all orders with filtering (Admin only)
         */
        List<OrderDto> getAllOrders(OrderFilter filter) throws CacheException, DataFactoryException;

        /**
         * Update order status
         */
        OrderDto updateOrderStatus(Long id, UpdateOrderStatusRequest request)
                        throws CacheException, DataFactoryException;

        /**
         * Assign driver to delivery order
         */
        OrderDto assignDriver(Long orderId, AssignDriverRequest request) throws CacheException, DataFactoryException;

        /**
         * Get kitchen queue (orders to prepare)
         */
        List<OrderDto> getKitchenQueue() throws CacheException, DataFactoryException;

        /**
         * Admin: Create dine-in order (no user authentication required)
         */
        OrderDto createDineInOrder(AdminCreateDineInRequest request) throws DataFactoryException;

        /**
         * Admin: Update payment status
         */
        OrderDto updatePaymentStatus(Long orderId, UpdatePaymentStatusRequest request)
                        throws CacheException, DataFactoryException;

        // ========== DELIVERY DRIVER OPERATIONS ==========

        /**
         * Get orders assigned to driver
         */
        List<OrderDto> getDriverAssignedOrders(String driverId) throws CacheException, DataFactoryException;

        /**
         * Mark order as completed (by driver after delivery)
         */
        OrderDto markOrderCompleted(Long orderId, String driverId) throws CacheException, DataFactoryException;

        // ========== PRE-ORDER (LINKED TO RESERVATION) ==========

        /**
         * Create pre-order linked to reservation
         */
        OrderDto createPreOrder(Long reservationId, CreateOrderRequest request, String userId)
                        throws DataFactoryException;

        /**
         * Create guest pre-order linked to reservation (no authentication required)
         */
        OrderDto createGuestPreOrder(Long reservationId, CreateOrderRequest request) throws DataFactoryException;

        // ========== EVENT HANDLING OPERATIONS ==========

        /**
         * Confirm pre-order when customer is seated (triggered by CustomerSeatedEvent)
         */
        void confirmPreOrder(Long orderId) throws CacheException, DataFactoryException;

        /**
         * Cancel order by orderId with reason (triggered by ReservationCancelledEvent)
         */
        void cancelOrderByEvent(Long orderId, String reason) throws CacheException, DataFactoryException;
}