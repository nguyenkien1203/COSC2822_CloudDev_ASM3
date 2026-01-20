// OrderProducerService.java (Interface)
package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.OrderDto;
import com.restaurant.orderservice.enums.OrderStatus;

public interface OrderProducerService {

    void publishOrderCreatedEvent(OrderDto order);

    void publishOrderStatusChangedEvent(OrderDto order, OrderStatus oldStatus, OrderStatus newStatus);

    void publishOrderCancelledEvent(OrderDto order, String reason);

    void publishDeliveryAssignedEvent(OrderDto order);

    void publishDeliveryCompletedEvent(OrderDto order);

    void publishPreOrderCreatedEvent(OrderDto order);

    void publishOrderCompletedEvent(OrderDto order);
}