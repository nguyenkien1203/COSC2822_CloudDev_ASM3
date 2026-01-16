package com.restaurant.reservationservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event DTO for when a pre-order is created for a reservation.
 * Received from Order Service.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PreOrderCreatedEvent extends BaseEvent {

    private Long orderId;
    private Long reservationId;
    private String userId;
    private BigDecimal totalAmount;
}
