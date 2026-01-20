package com.restaurant.reservationservice.service.impl;

import com.restaurant.reservationservice.dto.ReservationDto;
import com.restaurant.reservationservice.service.SnsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.format.DateTimeFormatter;

/**
 * Implementation of SNS notification service for sending emails
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SnsNotificationServiceImpl implements SnsNotificationService {

    private final SnsClient snsClient;

    @Value("${aws.sns.reservation-topic-arn}")
    private String reservationTopicArn;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    @Override
    public void sendReservationConfirmationEmail(ReservationDto reservation) {
        try {
            String customerName = getCustomerName(reservation);
            String customerEmail = getCustomerEmail(reservation);

            if (customerEmail == null || customerEmail.isBlank()) {
                log.warn("No email found for reservation {}, skipping notification", reservation.getId());
                return;
            }

            String subject = "Reservation Confirmation - " + reservation.getConfirmationCode();
            String message = buildEmailMessage(reservation, customerName);

            PublishRequest request = PublishRequest.builder()
                    .topicArn(reservationTopicArn)
                    .subject(subject)
                    .message(message)
                    .build();

            PublishResponse response = snsClient.publish(request);

            log.info("Sent reservation confirmation email for reservation {} to {}, messageId: {}",
                    reservation.getId(), customerEmail, response.messageId());

        } catch (Exception e) {
            log.error("Failed to send reservation confirmation email for reservation {}: {}",
                    reservation.getId(), e.getMessage(), e);
            // Don't throw - email failure shouldn't block reservation creation
        }
    }

    private String buildEmailMessage(ReservationDto reservation, String customerName) {
        StringBuilder sb = new StringBuilder();

        sb.append("Dear ").append(customerName).append(",\n\n");
        sb.append("Your reservation has been confirmed!\n\n");
        sb.append("═══════════════════════════════════════\n\n");
        sb.append("📅 Date: ").append(reservation.getReservationDate().format(DATE_FORMATTER)).append("\n");
        sb.append("🕐 Time: ").append(reservation.getStartTime().format(TIME_FORMATTER)).append("\n");
        sb.append("👥 Party Size: ").append(reservation.getPartySize()).append(" guests\n");

        if (reservation.getTable() != null) {
            sb.append("🪑 Table: ").append(reservation.getTable().getTableNumber()).append("\n");
        }

        sb.append("\n🔖 Confirmation Code: ").append(reservation.getConfirmationCode()).append("\n");

        if (reservation.getSpecialRequests() != null && !reservation.getSpecialRequests().isBlank()) {
            sb.append("\n📝 Special Requests: ").append(reservation.getSpecialRequests()).append("\n");
        }

        sb.append("\n═══════════════════════════════════════\n\n");

        // Include pre-order link if no pre-order exists
        if (reservation.getPreOrderId() == null) {
            sb.append("🍽️ SAVE TIME - PRE-ORDER YOUR MEAL!\n\n");
            sb.append("Beat the rush and have your food ready when you arrive.\n");
            sb.append("Click here to pre-order: ");
            sb.append(buildPreOrderUrl(reservation));
            sb.append("\n\n");
        }

        sb.append("Please present this confirmation code upon arrival.\n\n");
        sb.append("Thank you for choosing our restaurant!\n\n");
        sb.append("Best regards,\n");
        sb.append("The Restaurant Team\n");

        return sb.toString();
    }

    private String getCustomerName(ReservationDto reservation) {
        if (reservation.getGuestName() != null && !reservation.getGuestName().isBlank()) {
            return reservation.getGuestName();
        }
        // For logged-in users, we could fetch from profile service,
        // but for now just use a default
        return "Valued Customer";
    }

    private String getCustomerEmail(ReservationDto reservation) {
        // Guest email is stored directly on reservation
        if (reservation.getGuestEmail() != null && !reservation.getGuestEmail().isBlank()) {
            return reservation.getGuestEmail();
        }
        // For logged-in users, email would come from profile service
        // For now, return null (member reservations won't get email without profile
        // lookup)
        return null;
    }

    /**
     * Build the pre-order URL with query parameters matching frontend format
     */
    private String buildPreOrderUrl(ReservationDto reservation) {
        StringBuilder url = new StringBuilder(frontendUrl);
        url.append("/menu?");

        // Add reservation ID
        url.append("reservationId=").append(reservation.getId());

        // Add date as ISO datetime (frontend expects this)
        if (reservation.getReservationDate() != null && reservation.getStartTime() != null) {
            // Combine date and time into ISO format
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.of(
                    reservation.getReservationDate(),
                    reservation.getStartTime());
            url.append("&date=").append(urlEncode(dateTime.toString()));
        }

        // Add time (h:mm a format)
        if (reservation.getStartTime() != null) {
            String timeStr = reservation.getStartTime().format(TIME_FORMATTER);
            url.append("&time=").append(urlEncode(timeStr));
        }

        // Add party size
        url.append("&guests=").append(reservation.getPartySize());

        // Add guest name if available
        String name = getCustomerName(reservation);
        if (name != null && !name.isBlank()) {
            url.append("&name=").append(urlEncode(name));
        }

        // Add reservationDate (YYYY-MM-DD format)
        if (reservation.getReservationDate() != null) {
            url.append("&reservationDate=").append(reservation.getReservationDate().toString());
        }

        // Add reservationTime (h:mm a format)
        if (reservation.getStartTime() != null) {
            String timeStr = reservation.getStartTime().format(TIME_FORMATTER);
            url.append("&reservationTime=").append(urlEncode(timeStr));
        }

        return url.toString();
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
}
