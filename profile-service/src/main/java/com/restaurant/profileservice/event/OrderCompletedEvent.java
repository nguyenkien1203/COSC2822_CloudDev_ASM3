package com.restaurant.profileservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event received when order is completed (payment PAID + status COMPLETED)
 * Used to update loyalty points for members
 */
@Data
@SuperBuilder()
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCompletedEvent extends BaseEvent {

    private Long orderId;
    private String userId;
    private String orderType;
    private BigDecimal totalAmount;
    private String status;
    private String paymentStatus;
}
