package com.restaurant.profileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for membership information exposed to other services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipInfoDto {
    private String membershipRank; // SILVER, GOLD, PLATINUM, VIP
    private int discountPercentage; // 5, 10, 15, 20
    private int loyaltyPoints;
}
