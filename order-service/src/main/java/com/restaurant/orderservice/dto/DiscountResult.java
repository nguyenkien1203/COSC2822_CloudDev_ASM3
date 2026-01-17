package com.restaurant.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Result from Step Functions discount calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResult {
    private String membershipRank;
    private int discountPercentage;
    private BigDecimal discountAmount;
    private int loyaltyPoints;

    /**
     * Create a no-discount result for fallback scenarios
     */
    public static DiscountResult noDiscount() {
        return DiscountResult.builder()
                .membershipRank("SILVER")
                .discountPercentage(0)
                .discountAmount(BigDecimal.ZERO)
                .loyaltyPoints(0)
                .build();
    }
}
