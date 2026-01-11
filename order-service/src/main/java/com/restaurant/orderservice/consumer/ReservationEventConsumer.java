package com.restaurant.orderservice.consumer;

import com.restaurant.sqsmodule.config.SqsQueueConfig;
import com.restaurant.orderservice.event.CustomerSeatedEvent;
import com.restaurant.orderservice.event.ReservationCancelledEvent;
import com.restaurant.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for reservation-related events from reservation-service.
 * Handles events that affect orders (customer seating, reservation
 * cancellations).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventConsumer {

    private final OrderService orderService;

    /**
     * Handle customer seated event - enables dine-in ordering for this table.
     * If there's a pre-order linked, it can be started for preparation.
     */
    @SqsListener(queueNames = SqsQueueConfig.CUSTOMER_SEATED_QUEUE)
    public void handleCustomerSeated(@Payload CustomerSeatedEvent event) {

        try {
            log.info(
                    "Received CUSTOMER_SEATED event - eventId: {}, reservationId: {}, tableId: {}, preOrderId: {}",
                    event.getEventId(), event.getReservationId(), event.getTableId(),
                    event.getPreOrderId());

            // If there's a pre-order linked to this reservation, start preparation
            if (event.getPreOrderId() != null) {
                orderService.confirmPreOrder(event.getPreOrderId());
                log.info("Started preparation for pre-order: {} linked to reservation: {}",
                        event.getPreOrderId(), event.getReservationId());
            }

            log.info("Successfully processed CUSTOMER_SEATED event for reservation: {}",
                    event.getReservationId());

        } catch (Exception e) {
            log.error("Error handling CUSTOMER_SEATED event - eventId: {}, reservationId: {}",
                    event.getEventId(), event.getReservationId(), e);
            // Consider sending to DLQ (Dead Letter Queue) here
        }
    }

    /**
     * Handle reservation cancelled event - cancels any linked pre-orders.
     */
    @SqsListener(queueNames = SqsQueueConfig.RESERVATION_CANCELLED_QUEUE)
    public void handleReservationCancelled(@Payload ReservationCancelledEvent event) {

        try {
            log.info(
                    "Received RESERVATION_CANCELLED event - eventId: {}, reservationId: {}, preOrderId: {}, reason: {}",
                    event.getEventId(), event.getReservationId(), event.getPreOrderId(),
                    event.getReason());

            // Cancel the linked pre-order if exists
            if (event.getPreOrderId() != null) {
                String cancelReason = "Reservation cancelled: " +
                        (event.getReason() != null ? event.getReason() : "No reason provided");
                orderService.cancelOrderByEvent(event.getPreOrderId(), cancelReason);
                log.info("Cancelled pre-order: {} due to reservation cancellation", event.getPreOrderId());
            }

            log.info("Successfully processed RESERVATION_CANCELLED event for reservation: {}",
                    event.getReservationId());

        } catch (Exception e) {
            log.error("Error handling RESERVATION_CANCELLED event - eventId: {}, reservationId: {}",
                    event.getEventId(), event.getReservationId(), e);
            // Consider sending to DLQ (Dead Letter Queue) here
        }
    }
}
