package com.restaurant.orderservice.entity;

import com.restaurant.data.entity.IBaseEntity;
import com.restaurant.data.model.IBaseModel;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.enums.OrderType;
import com.restaurant.orderservice.enums.PaymentMethod;
import com.restaurant.orderservice.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity implements IBaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId; // Cognito sub (UUID string), NULL for guest orders

    @Column(name = "guest_email")
    private String guestEmail; // For guest orders

    @Column(name = "guest_phone")
    private String guestPhone; // For guest orders

    @Column(name = "guest_name")
    private String guestName; // For guest orders

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal; // Sum of all item prices before tax/fees

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount; // 8% of subtotal

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee; // Constant fee for DELIVERY orders

    @Column(name = "discount_percentage")
    private Integer discountPercentage; // Membership discount percentage (5, 10, 15, 20)

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount; // Calculated discount amount

    @Column(name = "membership_rank")
    private String membershipRank; // Applied membership rank (SILVER, GOLD, PLATINUM, VIP)

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount; // subtotal + taxAmount + deliveryFee - discountAmount

    @Column(name = "estimated_pickup_time")
    private LocalDateTime estimatedPickupTime; // User input for TAKEAWAY orders

    @Column(name = "delivery_address", length = 1000)
    private String deliveryAddress; // For delivery orders

    @Column(name = "reservation_id")
    private Long reservationId; // Link to reservation (for PRE_ORDER, DINE_IN)

    @Column(name = "driver_id")
    private String driverId; // Assigned delivery driver (Cognito sub)

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "estimated_ready_time")
    private LocalDateTime estimatedReadyTime;

    @Column(name = "actual_delivery_time")
    private LocalDateTime actualDeliveryTime;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItemEntity> orderItems = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to add order item
    public void addOrderItem(OrderItemEntity item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    // Helper method to remove order item
    public void removeOrderItem(OrderItemEntity item) {
        orderItems.remove(item);
        item.setOrder(null);
    }

    // Helper method to calculate total
    public void calculateTotal() {
        this.totalAmount = orderItems.stream()
                .map(OrderItemEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
