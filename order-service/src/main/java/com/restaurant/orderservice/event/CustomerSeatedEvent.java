package com.restaurant.orderservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event DTO for when a customer arrives and is seated at their table.
 * Received from Reservation Service.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerSeatedEvent extends BaseEvent {

    private Long reservationId;
    private Long tableId;
    private String tableNumber;
    private String userId;
    private Integer partySize;
    private Long preOrderId;
}
