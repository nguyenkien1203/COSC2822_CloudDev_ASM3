package com.restaurant.profileservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder()
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateOrderEvent extends BaseEvent {

    private Long orderId;
    private String userId;
    private String orderType;
    private BigDecimal totalAmount;
    private String status;
}
