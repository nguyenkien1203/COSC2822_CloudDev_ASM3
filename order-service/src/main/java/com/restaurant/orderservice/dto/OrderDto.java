package com.restaurant.orderservice.dto;

import com.restaurant.data.model.IBaseModel;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.enums.OrderType;
import com.restaurant.orderservice.enums.PaymentMethod;
import com.restaurant.orderservice.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto implements IBaseModel<Long> {

    private Long id;
    private String userId;
    private String guestEmail;
    private String guestPhone;
    private String guestName;
    private OrderType orderType;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private Long reservationId;
    private String driverId;
    private String notes;
    private LocalDateTime estimatedReadyTime;
    private LocalDateTime actualDeliveryTime;
    private List<OrderItemDto> orderItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
