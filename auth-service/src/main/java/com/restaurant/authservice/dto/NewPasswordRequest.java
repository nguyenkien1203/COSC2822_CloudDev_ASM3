package com.restaurant.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for setting new password (handles NEW_PASSWORD_REQUIRED
 * challenge)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewPasswordRequest {

    /**
     * User's email/username
     */
    private String email;

    /**
     * New password to set
     */
    private String newPassword;

    /**
     * Session from the NEW_PASSWORD_REQUIRED challenge
     */
    private String session;
}
