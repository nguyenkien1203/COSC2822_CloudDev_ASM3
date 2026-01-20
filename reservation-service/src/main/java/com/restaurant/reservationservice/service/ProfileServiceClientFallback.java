package com.restaurant.reservationservice.service;

import com.restaurant.reservationservice.dto.ProfileInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback for ProfileServiceClient when profile-service is unavailable
 * Returns null so reservation continues without email notification
 */
@Slf4j
@Component
public class ProfileServiceClientFallback implements ProfileServiceClient {

    @Override
    public ProfileInfoDto getProfileByUserId(String userId) {
        log.warn("Profile service unavailable, returning null for userId: {}", userId);
        return null;
    }
}
