package com.restaurant.orderservice.enums;

/**
 * Order lifecycle status
 */
public enum OrderStatus {
    PENDING,           // Order placed, awaiting confirmation
    CONFIRMED,         // Order confirmed by restaurant
    COMPLETED,         // Order fully completed (for dine-in)
    CANCELLED          // Order was cancelled
}
