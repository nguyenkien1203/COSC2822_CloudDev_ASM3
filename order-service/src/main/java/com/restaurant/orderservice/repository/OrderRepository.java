package com.restaurant.orderservice.repository;

import com.restaurant.orderservice.entity.OrderEntity;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.enums.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    // Find orders by user
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    // Find orders by status
    List<OrderEntity> findByStatus(OrderStatus status);

    // Find orders by type
    List<OrderEntity> findByOrderType(OrderType orderType);

    // Find orders assigned to driver
    List<OrderEntity> findByDriverIdAndStatusIn(String driverId, List<OrderStatus> statuses);

    // Find orders by reservation
    List<OrderEntity> findByReservationId(Long reservationId);

    // Kitchen queue - orders that need preparation
    @Query("SELECT o FROM OrderEntity o WHERE o.status IN :statuses ORDER BY o.createdAt ASC")
    List<OrderEntity> findKitchenQueue(@Param("statuses") List<OrderStatus> statuses);

    // Find orders within date range
    @Query("SELECT o FROM OrderEntity o WHERE o.createdAt BETWEEN :from AND :to ORDER BY o.createdAt DESC")
    List<OrderEntity> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Find guest orders by email
    List<OrderEntity> findByGuestEmailOrderByCreatedAtDesc(String guestEmail);

    // Count orders by status (for dashboard)
    Long countByStatus(OrderStatus status);

    // Check if user has any orders
    boolean existsByUserId(String userId);

    // Find delivery orders ready for pickup
    @Query("SELECT o FROM OrderEntity o WHERE o.orderType = 'DELIVERY' AND o.status = 'READY' AND o.driverId IS NULL")
    List<OrderEntity> findUnassignedDeliveryOrders();

    // Find order by ID with items eagerly loaded
    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.orderItems WHERE o.id = :orderId")
    java.util.Optional<OrderEntity> findByIdWithItems(@Param("orderId") Long orderId);
}
