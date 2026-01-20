package com.restaurant.reservationservice.service;

import com.restaurant.reservationservice.dto.ReservationDto;

/**
 * Service for sending SNS notifications
 */
public interface SnsNotificationService {

    /**
     * Send reservation confirmation email to customer
     * 
     * @param reservation The reservation details
     */
    void sendReservationConfirmationEmail(ReservationDto reservation);
}
