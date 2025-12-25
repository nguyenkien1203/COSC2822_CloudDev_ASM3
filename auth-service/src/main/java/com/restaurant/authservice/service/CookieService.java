package com.restaurant.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class CookieService {
    @Value("${jwt.cookie-name:auth_token}")
    private String cookieName;

    @Value("${jwt.expiration-days:7}")
    private int expirationDays;

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true) // 1. Hide from JavaScript (XSS Protection)
                .secure(false) // 2. Set to TRUE in Production (HTTPS only)
                .path("/") // 3. Available for the whole site
                .maxAge(expirationDays * 24 * 60 * 60) // Duration in seconds
                .sameSite("Lax") // 4. Prevent CSRF (Lax is more permissive than Strict for testing)
                .domain(null) // 5. Don't set domain to work with localhost
                .build();
    }

    public ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0) // Expire immediately
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from("refresh_" + cookieName, token)
                .httpOnly(true)
                .secure(false)
                .path("/api/auth") // Available for all auth endpoints
                .maxAge(30 * 24 * 60 * 60) // 30 days
                .sameSite("Lax")
                .domain(null)
                .build();
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from("refresh_" + cookieName, "")
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(0) // Expire immediately
                .build();
    }

    public String getRefreshTokenCookieName() {
        return "refresh_" + cookieName;
    }

    public String getAccessTokenCookieName() {
        return cookieName;
    }
}
