package com.restaurant.reservationservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a reservation is cancelled.
 * Consumed by Order Service to cancel any linked pre-orders.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReservationCancelledEvent extends BaseEvent {

    private Long reservationId;
    private String userId;
    private Long preOrderId; // Pre-order to cancel (if exists)
    private String reason;
}
