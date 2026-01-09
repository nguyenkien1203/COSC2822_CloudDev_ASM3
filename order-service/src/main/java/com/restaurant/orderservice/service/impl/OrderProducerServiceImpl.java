// OrderProducerServiceImpl.java
package com.restaurant.orderservice.service.impl;

import com.restaurant.sqsmodule.config.SqsQueueConfig;
import com.restaurant.sqsmodule.service.IBaseSqsProducer;
import com.restaurant.orderservice.dto.OrderDto;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.event.*;
import com.restaurant.orderservice.service.OrderProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducerServiceImpl implements OrderProducerService {

    private final IBaseSqsProducer sqsProducerService;

    @Value("${spring.application.name:order-service}")
    private String serviceName;

    // Topics/Queues are now used from SqsQueueConfig

    @Override
    public void publishOrderCreatedEvent(OrderDto order) {
        try {
            CreateOrderEvent event = CreateOrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_CREATED")
                    .timestamp(LocalDateTime.now())
                    .source(serviceName)
                    .version("1.0")
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .orderType(order.getOrderType().name())
                    .totalAmount(order.getTotalAmount())
                    .status(order.getStatus().name())
                    .build();

            sqsProducerService.sendEvent(SqsQueueConfig.ORDER_CREATED_QUEUE, event);
            log.info("Published ORDER_CREATED event for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to publish ORDER_CREATED event for order: {}", order.getId(), e);
        }
    }

    @Override
    public void publishOrderStatusChangedEvent(OrderDto order, OrderStatus oldStatus, OrderStatus newStatus) {
        try {
            OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_STATUS_CHANGED")
                    .timestamp(LocalDateTime.now())
                    .source(serviceName)
                    .version("1.0")
                    .orderId(order.getId())
                    .oldStatus(oldStatus.name())
                    .newStatus(newStatus.name())
                    .build();

            sqsProducerService.sendEvent(SqsQueueConfig.ORDER_UPDATED_QUEUE, event);
            log.info("Published ORDER_STATUS_CHANGED event for order: {} ({} -> {})",
                    order.getId(), oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to publish ORDER_STATUS_CHANGED event for order: {}", order.getId(), e);
        }
    }

    @Override
    public void publishOrderCancelledEvent(OrderDto order, String reason) {
        try {
            CancelOrderEvent event = CancelOrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_CANCELLED")
                    .timestamp(LocalDateTime.now())
                    .source(serviceName)
                    .version("1.0")
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .reason(reason)
                    .build();

            sqsProducerService.sendEvent(SqsQueueConfig.ORDER_CANCELLED_QUEUE, event);
            log.info("Published ORDER_CANCELLED event for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to publish ORDER_CANCELLED event for order: {}", order.getId(), e);
        }
    }

    @Override
    public void publishDeliveryAssignedEvent(OrderDto order) {
        try {
            DeliveryAssignedEvent event = DeliveryAssignedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("DELIVERY_ASSIGNED")
                    .timestamp(LocalDateTime.now())
                    .source(serviceName)
                    .version("1.0")
                    .orderId(order.getId())
                    .driverId(order.getDriverId())
                    .deliveryAddress(order.getDeliveryAddress())
                    .customerPhone(order.getGuestPhone())
                    .build();

            sqsProducerService.sendEvent(SqsQueueConfig.DELIVERY_ASSIGNED_QUEUE, event);
            log.info("Published DELIVERY_ASSIGNED event for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to publish DELIVERY_ASSIGNED event for order: {}", order.getId(), e);
        }
    }

    @Override
    public void publishDeliveryCompletedEvent(OrderDto order) {
        try {
            DeliveryCompletedEvent event = DeliveryCompletedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("DELIVERY_COMPLETED")
                    .timestamp(LocalDateTime.now())
                    .source(serviceName)
                    .version("1.0")
                    .orderId(order.getId())
                    .driverId(order.getDriverId())
                    .deliveredAt(order.getActualDeliveryTime())
                    .build();

            sqsProducerService.sendEvent(SqsQueueConfig.DELIVERY_COMPLETED_QUEUE, event);
            log.info("Published DELIVERY_COMPLETED event for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to publish DELIVERY_COMPLETED event for order: {}", order.getId(), e);
        }
    }

    @Override
    public void publishPreOrderCreatedEvent(OrderDto order) {
        try {
            PreOrderCreatedEvent event = PreOrderCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PRE_ORDER_CREATED")
                    .timestamp(LocalDateTime.now())
                    .source(serviceName)
                    .version("1.0")
                    .orderId(order.getId())
                    .reservationId(order.getReservationId())
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalAmount())
                    .build();

            sqsProducerService.sendEvent(SqsQueueConfig.PRE_ORDER_CREATED_QUEUE, event);
            log.info("Published PRE_ORDER_CREATED event for order: {} linked to reservation: {}",
                    order.getId(), order.getReservationId());
        } catch (Exception e) {
            log.error("Failed to publish PRE_ORDER_CREATED event for order: {}", order.getId(), e);
        }
    }
}