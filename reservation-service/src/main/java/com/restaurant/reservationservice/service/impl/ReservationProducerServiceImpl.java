package com.restaurant.reservationservice.service.impl;

import com.restaurant.sqsmodule.config.SqsQueueConfig;
import com.restaurant.sqsmodule.service.BaseSqsProducer;
import com.restaurant.reservationservice.dto.ReservationDto;
import com.restaurant.reservationservice.event.*;
import com.restaurant.reservationservice.service.ReservationProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of ReservationProducerService for publishing Kafka events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationProducerServiceImpl implements ReservationProducerService {

    private final BaseSqsProducer sqsProducerService;

    @Value("${spring.application.name:reservation-service}")
    private String serviceName;

    @Override
    public void publishCustomerSeatedEvent(ReservationDto reservation) {
        try {
            CustomerSeatedEvent event = CustomerSeatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("CUSTOMER_SEATED")
                    .timestamp(LocalDateTime.now())
                    .source(serviceName)
                    .version("1.0")
                    .reservationId(reservation.getId())
                    .tableId(reservation.getTable() != null ? reservation.getTable().getId() : null)
                    .tableNumber(reservation.getTable() != null ? reservation.getTable().getTableNumber() : null)
                    .userId(reservation.getUserId())
                    .partySize(reservation.getPartySize())
                    .preOrderId(reservation.getPreOrderId())
                    .build();

            sqsProducerService.sendEvent(SqsQueueConfig.CUSTOMER_SEATED_QUEUE, event);
            log.info("Published CUSTOMER_SEATED event for reservation: {}", reservation.getId());
        } catch (Exception e) {
            log.error("Failed to publish CUSTOMER_SEATED event for reservation: {}", reservation.getId(), e);
        }
    }

    @Override
    public void publishReservationCancelledEvent(ReservationDto reservation, String reason) {
        try {
            ReservationCancelledEvent event = ReservationCancelledEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("RESERVATION_CANCELLED")
                    .timestamp(LocalDateTime.now())
                    .source(serviceName)
                    .version("1.0")
                    .reservationId(reservation.getId())
                    .userId(reservation.getUserId())
                    .preOrderId(reservation.getPreOrderId())
                    .reason(reason)
                    .build();

            sqsProducerService.sendEvent(SqsQueueConfig.RESERVATION_CANCELLED_QUEUE, event);
            log.info("Published RESERVATION_CANCELLED event for reservation: {}", reservation.getId());
        } catch (Exception e) {
            log.error("Failed to publish RESERVATION_CANCELLED event for reservation: {}", reservation.getId(), e);
        }
    }

    @Override
    public void publishReservationCompletedEvent(ReservationDto reservation) {
        try {
            ReservationCompletedEvent event = ReservationCompletedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("RESERVATION_COMPLETED")
                    .timestamp(LocalDateTime.now())
                    .source(serviceName)
                    .version("1.0")
                    .reservationId(reservation.getId())
                    .tableId(reservation.getTable() != null ? reservation.getTable().getId() : null)
                    .userId(reservation.getUserId())
                    .build();

            sqsProducerService.sendEvent(SqsQueueConfig.RESERVATION_COMPLETED_QUEUE, event);
            log.info("Published RESERVATION_COMPLETED event for reservation: {}", reservation.getId());
        } catch (Exception e) {
            log.error("Failed to publish RESERVATION_COMPLETED event for reservation: {}", reservation.getId(), e);
        }
    }
}
