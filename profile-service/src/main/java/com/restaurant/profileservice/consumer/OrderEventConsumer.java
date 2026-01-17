package com.restaurant.profileservice.consumer;

import com.restaurant.sqsmodule.config.SqsQueueConfig;
import com.restaurant.profileservice.event.CreateOrderEvent;
import com.restaurant.profileservice.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SQS consumer for order-related events from order-service
 * Updates loyalty points when members create orders
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ProfileService profileService;

    private static final BigDecimal LOYALTY_POINT_PERCENTAGE = new BigDecimal("0.10"); // 10%

    /**
     * Listen to order created events and update loyalty points for members
     * Only processes orders from logged-in users (members), not guest orders
     */
    @SqsListener(queueNames = SqsQueueConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(@Payload CreateOrderEvent event) {
        try {
            log.info("Received ORDER_CREATED event - eventId: {}, orderId: {}, userId: {}, totalAmount: {}",
                    event.getEventId(), event.getOrderId(), event.getUserId(), event.getTotalAmount());

            // Only process orders from logged-in members (userId is not null)
            if (event.getUserId() == null || event.getUserId().trim().isEmpty()) {
                log.debug("Skipping loyalty points update for guest order - orderId: {}", event.getOrderId());
                return;
            }

            // Only process if order has a valid total amount
            if (event.getTotalAmount() == null || event.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Invalid total amount for order - orderId: {}, totalAmount: {}",
                        event.getOrderId(), event.getTotalAmount());
                return;
            }

            // Calculate loyalty points: 10% of order total
            BigDecimal pointsToAdd = event.getTotalAmount()
                    .multiply(LOYALTY_POINT_PERCENTAGE)
                    .setScale(0, RoundingMode.HALF_UP);

            log.info("Calculated loyalty points to add: {} for order: {}, userId: {}",
                    pointsToAdd.intValue(), event.getOrderId(), event.getUserId());

            // Update loyalty points
            profileService.updateLoyaltyPoints(event.getUserId(), pointsToAdd.intValue());

            log.info("Successfully updated loyalty points for user: {}, orderId: {}",
                    event.getUserId(), event.getOrderId());

        } catch (Exception e) {
            log.error("Error handling ORDER_CREATED event - eventId: {}, orderId: {}, userId: {}",
                    event.getEventId(), event.getOrderId(), event.getUserId(), e);
        }
    }
}
