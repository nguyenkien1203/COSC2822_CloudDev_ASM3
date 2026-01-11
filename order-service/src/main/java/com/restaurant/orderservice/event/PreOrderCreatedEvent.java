package com.restaurant.orderservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event published when a pre-order is created for a reservation.
 * Consumed by Reservation Service to link the pre-order to the reservation.
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
