package com.restaurant.profileservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restaurant.sqsmodule.config.SqsQueueConfig;
import com.restaurant.profileservice.event.OrderCompletedEvent;
import com.restaurant.profileservice.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SQS consumer for order-related events from order-service
 * Updates loyalty points when orders are COMPLETED and PAID
 */
@Slf4j
@Component
public class OrderEventConsumer {

    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    private static final BigDecimal LOYALTY_POINT_PERCENTAGE = new BigDecimal("0.10"); // 10%

    public OrderEventConsumer(ProfileService profileService) {
        this.profileService = profileService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Listen to order completed events and update loyalty points for members
     * Only processes orders from logged-in users (members), not guest orders
     * Triggered when payment is PAID and order status is COMPLETED
     * Receives raw String payload to avoid JavaType header issues between services
     */
    @SqsListener(queueNames = SqsQueueConfig.ORDER_COMPLETED_QUEUE)
    public void handleOrderCompleted(@Payload String payload) {
        try {
            log.info("Received ORDER_COMPLETED event payload: {}", payload);

            // Manually deserialize to avoid JavaType header mismatch
            OrderCompletedEvent event = objectMapper.readValue(payload, OrderCompletedEvent.class);

            log.info(
                    "Parsed ORDER_COMPLETED event - orderId: {}, userId: {}, totalAmount: {}, status: {}, paymentStatus: {}",
                    event.getOrderId(), event.getUserId(), event.getTotalAmount(),
                    event.getStatus(), event.getPaymentStatus());

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
            log.error("Error handling ORDER_COMPLETED event - payload: {}", payload, e);
        }
    }
}
