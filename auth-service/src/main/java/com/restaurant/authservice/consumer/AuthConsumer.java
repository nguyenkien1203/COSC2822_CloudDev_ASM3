package com.restaurant.authservice.consumer;

import com.restaurant.authservice.event.DeleteProfileEvent;
import com.restaurant.authservice.service.AuthService;
import com.restaurant.sqsmodule.config.SqsQueueConfig;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthConsumer {

    private final AuthService authService;

    @SqsListener(queueNames = SqsQueueConfig.PROFILE_DELETED_QUEUE)
    public void handleUserRegistered(@Payload DeleteProfileEvent event) {

        try {
            log.info("Received PROFILE_DELETE event - eventId: {}, userId: {}",
                    event.getEventId(), event.getUserId());

            // Auto-delete profile

            authService.deleteAuthRecord(event.getUserId());

            log.info("Successfully delete record for userId: {}", event.getUserId());

        } catch (Exception e) {
            log.error("Error handling USER_REGISTERED event - eventId: {}, userId: {}",
                    event.getEventId(), event.getUserId(), e);
            // You might want to send to DLQ (Dead Letter Queue) here
        }
    }

}
