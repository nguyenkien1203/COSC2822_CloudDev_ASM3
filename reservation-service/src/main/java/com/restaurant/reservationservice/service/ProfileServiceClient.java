package com.restaurant.reservationservice.service;

import com.restaurant.reservationservice.config.FeignClientConfig;
import com.restaurant.reservationservice.dto.ProfileInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client to call profile-service for user information
 * Used to fetch user email for SNS notifications
 */
@FeignClient(name = "profile-service", url = "${profile-service.url:}", configuration = FeignClientConfig.class, fallback = ProfileServiceClientFallback.class)
public interface ProfileServiceClient {

    /**
     * Get profile by userId using public internal endpoint
     */
    @GetMapping("/api/profiles/internal/user/{userId}")
    ProfileInfoDto getProfileByUserId(@PathVariable("userId") String userId);
}
