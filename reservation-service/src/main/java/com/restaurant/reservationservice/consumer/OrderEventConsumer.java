package com.restaurant.reservationservice.consumer;

import com.restaurant.sqsmodule.config.SqsQueueConfig;
import com.restaurant.reservationservice.event.PreOrderCreatedEvent;
import com.restaurant.reservationservice.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order-related events from order-service.
 * Handles events that affect reservations (pre-order creation).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ReservationService reservationService;

    /**
     * Handle pre-order created event - links the pre-order to the reservation.
     */
    @SqsListener(queueNames = SqsQueueConfig.PRE_ORDER_CREATED_QUEUE)
    public void handlePreOrderCreated(@Payload PreOrderCreatedEvent event) {

        try {
            log.info(
                    "Received PRE_ORDER_CREATED event - eventId: {}, orderId: {}, reservationId: {}",
                    event.getEventId(), event.getOrderId(), event.getReservationId());

            if (event.getReservationId() == null) {
                log.warn("Pre-order {} has no reservation ID, skipping link", event.getOrderId());
                return;
            }

            // Link pre-order to reservation
            reservationService.linkPreOrder(event.getReservationId(), event.getOrderId());

            log.info("Successfully linked pre-order {} to reservation {}",
                    event.getOrderId(), event.getReservationId());

        } catch (Exception e) {
            log.error("Error handling PRE_ORDER_CREATED event - eventId: {}, orderId: {}, reservationId: {}",
                    event.getEventId(), event.getOrderId(), event.getReservationId(), e);
            // Consider sending to DLQ (Dead Letter Queue) here
        }
    }
}
