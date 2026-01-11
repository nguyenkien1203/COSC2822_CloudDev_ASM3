package com.restaurant.orderservice.filter;

import com.restaurant.data.model.IFilter;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.enums.OrderType;
import com.restaurant.orderservice.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilter implements IFilter {

    private Long id;
    private String userId;
    private String driverId;
    private Long reservationId;
    private OrderType orderType;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private String guestEmail;
    private String guestPhone;
}
