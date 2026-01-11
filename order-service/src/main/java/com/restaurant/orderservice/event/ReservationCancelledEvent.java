package com.restaurant.orderservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event DTO for when a reservation is cancelled.
 * Received from Reservation Service.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReservationCancelledEvent extends BaseEvent {

    private Long reservationId;
    private String userId;
    private Long preOrderId;
    private String reason;
}
