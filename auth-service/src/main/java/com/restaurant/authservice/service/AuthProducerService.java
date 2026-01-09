package com.restaurant.authservice.service;

import com.restaurant.authservice.event.LoginEvent;
import com.restaurant.sqsmodule.event.RegisterEvent;
import com.restaurant.authservice.event.TokenRefreshEvent;
import com.restaurant.authservice.event.UserLogoutEvent;


/**
 * Service for publishing auth-related events to Kafka
 */
public interface AuthProducerService {
    
    void publishUserRegisteredEvent(RegisterEvent event);

    void publishUserLoginEvent(LoginEvent event);
    
    void publishUserLogoutEvent(UserLogoutEvent event);
    
    void publishTokenRefreshedEvent(TokenRefreshEvent event);
}

