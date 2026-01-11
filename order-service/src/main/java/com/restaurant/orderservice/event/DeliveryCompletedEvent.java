// DeliveryCompletedEvent.java
package com.restaurant.orderservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder()
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DeliveryCompletedEvent extends BaseEvent {

    private Long orderId;
    private String driverId;
    private LocalDateTime deliveredAt;
}