package com.restaurant.profileservice.consumer;

import com.restaurant.sqsmodule.config.SqsQueueConfig;
import com.restaurant.sqsmodule.event.RegisterEvent;
import com.restaurant.profileservice.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * SQS consumer for user-related events from auth-service
 * Automatically creates profiles when users register
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final ProfileService profileService;

    /**
     * Listen to user registration events and auto-create profiles
     * Uses cognitoSub as the user identifier since Cognito doesn't use numeric IDs
     */
    @SqsListener(queueNames = SqsQueueConfig.USER_REGISTERED_QUEUE)
    public void handleUserRegistered(@Payload RegisterEvent event) {

        try {
            log.info("Received USER_REGISTERED event - eventId: {}, cognitoSub: {}, email: {}, fullname: {}",
                    event.getEventId(), event.getCognitoSub(), event.getEmail(), event.getFullName());

            // Use cognitoSub as the unique identifier, email as fallback
            String userIdentifier = event.getCognitoSub() != null ? event.getCognitoSub() : event.getEmail();
            
            // Auto-create profile using cognitoSub/email as identifier
            profileService.createProfileFromUserRegistration(
                    userIdentifier, 
                    event.getEmail(), 
                    event.getFullName(),
                    event.getPhone(), 
                    event.getAddress());

            log.info("Successfully created profile for user: {}", event.getEmail());

        } catch (Exception e) {
            log.error("Error handling USER_REGISTERED event - eventId: {}, email: {}",
                    event.getEventId(), event.getEmail(), e);
        }
    }
}
