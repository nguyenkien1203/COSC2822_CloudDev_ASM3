package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.MembershipInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback for ProfileServiceClient when profile-service is unavailable
 * Returns default values (no discount) so orders can still be created
 */
@Slf4j
@Component
public class ProfileServiceClientFallback implements ProfileServiceClient {

    @Override
    public MembershipInfoDto getMembershipInfo(String userId) {
        log.warn("Profile service unavailable, returning default membership info (no discount) for userId: {}", userId);
        return MembershipInfoDto.builder()
                .membershipRank("SILVER")
                .discountPercentage(0) // No discount when service is unavailable
                .loyaltyPoints(0)
                .build();
    }
}
