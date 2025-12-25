package com.restaurant.authservice.controller;

import com.restaurant.authservice.dto.*;
import com.restaurant.authservice.service.CognitoAuthService;
import com.restaurant.authservice.service.CognitoJwtValidator;
import com.restaurant.authservice.service.CookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * Authentication controller using AWS Cognito
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private CognitoAuthService cognitoAuthService;

    @Autowired
    private CookieService cookieService;

    @Autowired
    private CognitoJwtValidator cognitoJwtValidator;

    /**
     * Login with Cognito
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for: {}", loginRequest.getEmail());

            CognitoAuthResponse authResponse = cognitoAuthService.login(
                    loginRequest.getEmail(),
                    loginRequest.getPassword());

            // Check if user needs to set a new password
            if (authResponse.isRequiresNewPassword()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(Map.of(
                                "challenge", "NEW_PASSWORD_REQUIRED",
                                "session", authResponse.getSession(),
                                "email", authResponse.getEmail(),
                                "message", "Please set a new password"));
            }

            // Create cookies with Cognito tokens
            // Use ID token for auth cookie (contains user claims)
            ResponseCookie accessCookie = cookieService.createAccessTokenCookie(authResponse.getIdToken());
            ResponseCookie refreshCookie = cookieService.createRefreshTokenCookie(authResponse.getRefreshToken());

            AuthResponseDto responseDto = AuthResponseDto.builder()
                    .email(authResponse.getEmail())
                    .active(true)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(responseDto);

        } catch (RuntimeException e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed", "message", "An unexpected error occurred"));
        }
    }

    /**
     * Respond to NEW_PASSWORD_REQUIRED challenge
     */
    @PostMapping("/set-password")
    public ResponseEntity<?> setNewPassword(@RequestBody NewPasswordRequest request) {
        try {
            log.info("Setting new password for: {}", request.getEmail());

            CognitoAuthResponse authResponse = cognitoAuthService.respondToNewPasswordChallenge(request);

            // Create cookies with Cognito tokens
            ResponseCookie accessCookie = cookieService.createAccessTokenCookie(authResponse.getIdToken());
            ResponseCookie refreshCookie = cookieService.createRefreshTokenCookie(authResponse.getRefreshToken());

            AuthResponseDto responseDto = AuthResponseDto.builder()
                    .email(authResponse.getEmail())
                    .active(true)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(responseDto);

        } catch (Exception e) {
            log.error("Set password failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to set password", "message", e.getMessage()));
        }
    }

    /**
     * Logout from Cognito
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            // Get access token from cookie to revoke it in Cognito
            String accessToken = getTokenFromCookie(request, cookieService.getAccessTokenCookieName());

            if (accessToken != null) {
                // Try to get email for event publishing
                try {
                    CognitoJwtValidator.CognitoClaims claims = cognitoJwtValidator.validateToken(accessToken);
                    cognitoAuthService.publishLogoutEvent(claims.getEmail());
                } catch (Exception e) {
                    log.debug("Could not extract claims for logout event");
                }

                // Sign out from Cognito
                cognitoAuthService.logout(accessToken);
            }
        } catch (Exception e) {
            log.warn("Error during Cognito logout: {}", e.getMessage());
            // Continue to delete cookies even if Cognito logout fails
        }

        // Delete cookies
        ResponseCookie accessCookie = cookieService.deleteAccessTokenCookie();
        ResponseCookie refreshCookie = cookieService.deleteRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "Logout successful"));
    }

    /**
     * Refresh access token using Cognito refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        try {
            String refreshToken = getTokenFromCookie(request, cookieService.getRefreshTokenCookieName());

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token not found"));
            }

            // Get email from current ID token (if available)
            String email = null;
            String currentIdToken = getTokenFromCookie(request, cookieService.getAccessTokenCookieName());
            if (currentIdToken != null) {
                try {
                    CognitoJwtValidator.CognitoClaims claims = cognitoJwtValidator.validateToken(currentIdToken);
                    email = claims.getEmail();
                } catch (Exception e) {
                    log.debug("Could not extract email from expired token");
                }
            }

            CognitoAuthResponse authResponse = cognitoAuthService.refreshToken(refreshToken, email);

            // Create new access cookie with new ID token
            ResponseCookie accessCookie = cookieService.createAccessTokenCookie(authResponse.getIdToken());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(Map.of(
                            "message", "Token refreshed successfully",
                            "email", authResponse.getEmail() != null ? authResponse.getEmail() : ""));

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token refresh failed", "message", e.getMessage()));
        }
    }

    /**
     * Get current authenticated user info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }

            String email = authentication.getName();
            String roles = authentication.getAuthorities().stream()
                    .map(Object::toString)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            return ResponseEntity.ok(Map.of(
                    "email", email,
                    "roles", roles,
                    "authenticated", true));

        } catch (Exception e) {
            log.error("Error fetching current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user information"));
        }
    }

    /**
     * Register new user in Cognito
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDto registerRequest) {
        try {
            // Validate input
            if (registerRequest.getEmail() == null || registerRequest.getEmail().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email is required"));
            }
            if (registerRequest.getPassword() == null || registerRequest.getPassword().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password is required"));
            }

            // Register user via Cognito
            CognitoAuthResponse authResponse = cognitoAuthService.register(registerRequest);

            // Create cookies with tokens (auto-login after registration)
            ResponseCookie accessCookie = cookieService.createAccessTokenCookie(authResponse.getIdToken());
            ResponseCookie refreshCookie = cookieService.createRefreshTokenCookie(authResponse.getRefreshToken());

            AuthResponseDto responseDto = AuthResponseDto.builder()
                    .email(authResponse.getEmail())
                    .active(true)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(responseDto);

        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Registration failed", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed", "message", "An unexpected error occurred"));
        }
    }

    /**
     * Helper method to extract token from cookie
     */
    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
