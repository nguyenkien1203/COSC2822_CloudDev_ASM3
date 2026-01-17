package com.restaurant.orderservice.service;

import com.restaurant.orderservice.config.FeignClientConfig;
import com.restaurant.orderservice.dto.MembershipInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client to call profile-service for membership information
 */
@FeignClient(name = "profile-service", url = "${profile-service.url:}", configuration = FeignClientConfig.class, fallback = ProfileServiceClientFallback.class)
public interface ProfileServiceClient {

    /**
     * Get membership info for a user by their userId (Cognito sub)
     */
    @GetMapping("/api/profiles/user/{userId}/membership")
    MembershipInfoDto getMembershipInfo(@PathVariable("userId") String userId);
}
