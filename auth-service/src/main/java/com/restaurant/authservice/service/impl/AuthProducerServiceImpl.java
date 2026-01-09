package com.restaurant.authservice.service.impl;

import com.restaurant.authservice.event.LoginEvent;
import com.restaurant.sqsmodule.event.RegisterEvent;
import com.restaurant.authservice.event.TokenRefreshEvent;
import com.restaurant.authservice.event.UserLogoutEvent;
import com.restaurant.authservice.service.AuthProducerService;
import com.restaurant.sqsmodule.config.SqsQueueConfig;
import com.restaurant.sqsmodule.service.BaseSqsProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of AuthKafkaProducerService
 * Uses BaseKafkaProducer from kafka-module for actual message sending
 */
@Slf4j
@Service
public class AuthProducerServiceImpl implements AuthProducerService {

    private final BaseSqsProducer sqsProducer;

    public AuthProducerServiceImpl(BaseSqsProducer sqsProducer) {
        this.sqsProducer = sqsProducer;
    }

    @Override
    public void publishUserRegisteredEvent(RegisterEvent event) {
        log.debug("Publishing user registered event for userId: {}", event.getId());
        sqsProducer.sendEvent(
                SqsQueueConfig.USER_REGISTERED_QUEUE,
                event);
    }

    @Override
    public void publishUserLoginEvent(LoginEvent event) {
        log.debug("Publishing user login event for userId: {}", event.getId());
        sqsProducer.sendEvent(
                SqsQueueConfig.USER_LOGIN_QUEUE,
                event);
    }

    @Override
    public void publishUserLogoutEvent(UserLogoutEvent event) {
        log.debug("Publishing user logout event for userId: {}", event.getUserId());
        sqsProducer.sendEvent(
                SqsQueueConfig.USER_LOGOUT_QUEUE,
                event);
    }

    @Override
    public void publishTokenRefreshedEvent(TokenRefreshEvent event) {
        log.debug("Publishing token refreshed event for userId: {}", event.getId());
        sqsProducer.sendEvent(
                SqsQueueConfig.TOKEN_REFRESHED_QUEUE,
                event);
    }
}
