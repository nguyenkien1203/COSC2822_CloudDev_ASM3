package com.restaurant.reservationservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a reservation is completed (dining session ends).
 * Can be consumed by Order Service to finalize any open orders.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReservationCompletedEvent extends BaseEvent {

    private Long reservationId;
    private Long tableId;
    private String userId;
}
