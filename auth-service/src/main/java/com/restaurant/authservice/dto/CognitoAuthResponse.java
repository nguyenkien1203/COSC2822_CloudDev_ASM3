package com.restaurant.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Cognito authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CognitoAuthResponse {

    /**
     * Cognito access token (JWT)
     */
    private String accessToken;

    /**
     * Cognito ID token (contains user claims)
     */
    private String idToken;

    /**
     * Cognito refresh token
     */
    private String refreshToken;

    /**
     * Token expiration time in seconds
     */
    private Integer expiresIn;

    /**
     * Token type (usually "Bearer")
     */
    private String tokenType;

    /**
     * User's email
     */
    private String email;

    /**
     * Cognito username (sub)
     */
    private String username;

    /**
     * Indicates if user needs to change password on first login
     */
    private boolean requiresNewPassword;

    /**
     * Session ID for password change flow
     */
    private String session;

    /**
     * Challenge name if authentication requires additional steps
     */
    private String challengeName;
}
