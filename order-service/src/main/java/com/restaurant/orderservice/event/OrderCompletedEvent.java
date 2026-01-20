package com.restaurant.orderservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event published when an order is completed (payment PAID + status COMPLETED)
 * Used to trigger loyalty points update in profile-service
 */
@Data
@SuperBuilder()
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCompletedEvent extends BaseEvent {

    private Long orderId;
    private String userId;
    private String orderType;
    private BigDecimal totalAmount;
    private String status;
    private String paymentStatus;
}
