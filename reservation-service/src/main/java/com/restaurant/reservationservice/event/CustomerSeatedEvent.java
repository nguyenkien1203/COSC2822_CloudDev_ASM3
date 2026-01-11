package com.restaurant.reservationservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a customer arrives and is seated at their table.
 * Consumed by Order Service to enable dine-in ordering for this table.
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
    private Long preOrderId; // Link to existing pre-order if any
}
