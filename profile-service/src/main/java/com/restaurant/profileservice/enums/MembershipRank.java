package com.restaurant.profileservice.enums;

/**
 * Membership rank tiers with respective discount percentages
 */
public enum MembershipRank {
    SILVER(5), // 5% discount
    GOLD(10), // 10% discount
    PLATINUM(15), // 15% discount
    VIP(20); // 20% discount

    private final int discountPercentage;

    MembershipRank(int discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public int getDiscountPercentage() {
        return discountPercentage;
    }
}
