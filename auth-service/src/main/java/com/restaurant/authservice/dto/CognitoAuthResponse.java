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

    private String accessToken;
    private String idToken;
    private String refreshToken;
    private Integer expiresIn;
    private String tokenType;
    private String email;
    private String username;
    private String sub;
    private boolean requiresNewPassword;
    private boolean requiresConfirmation;
    private String session;
    private String challengeName;
    private String message;
}
