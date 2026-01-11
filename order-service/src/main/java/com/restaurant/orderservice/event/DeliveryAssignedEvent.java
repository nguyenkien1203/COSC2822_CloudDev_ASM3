// DeliveryAssignedEvent.java
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
public class DeliveryAssignedEvent extends BaseEvent {

    private Long orderId;
    private String driverId;
    private String deliveryAddress;
    private String customerPhone;
}