package com.restaurant.sqsmodule.config;

import org.springframework.context.annotation.Configuration;

/**
 * SQS Queue Configuration
 * Defines common queue names used across microservices
 */
@Configuration
public class SqsQueueConfig {

    // Queue name constants
    public static final String USER_REGISTERED_QUEUE = "user-registered";
    public static final String USER_LOGIN_QUEUE = "user-login";
    public static final String USER_LOGOUT_QUEUE = "user-logout";
    public static final String TOKEN_REFRESHED_QUEUE = "token-refreshed";
    public static final String PROFILE_UPDATED_QUEUE = "profile-updated";
    public static final String PROFILE_DELETED_QUEUE = "profile-deleted";
    public static final String ORDER_CREATED_QUEUE = "order-created";
    public static final String ORDER_UPDATED_QUEUE = "order-updated";
    public static final String ORDER_CANCELLED_QUEUE = "order-cancelled";
    public static final String DELIVERY_ASSIGNED_QUEUE = "delivery-assigned";
    public static final String DELIVERY_COMPLETED_QUEUE = "delivery-completed";
    public static final String RESERVATION_CREATED_QUEUE = "reservation-created";
    public static final String RESERVATION_UPDATED_QUEUE = "reservation-updated";
    public static final String MENU_UPDATED_QUEUE = "menu-updated";
    public static final String TABLE_UPDATED_QUEUE = "table-updated";
    public static final String CUSTOMER_SEATED_QUEUE = "reservation-customer-seated";
    public static final String RESERVATION_CANCELLED_QUEUE = "reservation-cancelled";
    public static final String RESERVATION_COMPLETED_QUEUE = "reservation-completed";
    public static final String PRE_ORDER_CREATED_QUEUE = "order-pre-order-created";
}
