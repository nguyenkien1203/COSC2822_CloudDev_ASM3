package com.restaurant.securitymodule.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.restaurant.securitymodule.config.SecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Service for validating Cognito JWT tokens using JWKS
 * This is the shared validator for all microservices
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoJwtValidator {

    private final SecurityProperties securityProperties;
    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        try {
            String jwksUrl = securityProperties.getCognito().getJwksUrl();
            if (jwksUrl == null || jwksUrl.isEmpty()) {
                log.warn("Cognito JWKS URL not configured. JWT validation will not be available.");
                return;
            }

            // Fetch JWKS from Cognito
            JWKSet jwkSet = JWKSet.load(new URL(jwksUrl));
            JWKSource<SecurityContext> keySource = new ImmutableJWKSet<>(jwkSet);

            // Configure processor for RS256 algorithm (Cognito default)
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                    JWSAlgorithm.RS256, keySource);

            jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(keySelector);

            initialized = true;
            log.info("Cognito JWT validator initialized with JWKS from: {}", jwksUrl);
        } catch (Exception e) {
            log.error("Failed to initialize Cognito JWT validator: {}", e.getMessage());
            // Don't throw - allow service to start, but JWT validation will fail
        }
    }

    /**
     * Check if validator is properly initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Validate and parse a Cognito JWT token
     *
     * @param token JWT token (access token or ID token)
     * @return CognitoClaims with extracted user information
     * @throws RuntimeException if token is invalid or validator not initialized
     */
    public CognitoClaims validateToken(String token) {
        if (!initialized) {
            throw new RuntimeException("Cognito JWT validator not initialized");
        }

        try {
            JWTClaimsSet claimsSet = jwtProcessor.process(token, null);

            // Validate issuer
            String issuer = claimsSet.getIssuer();
            String expectedIssuer = securityProperties.getCognito().getIssuerUrl();
            if (expectedIssuer != null && !expectedIssuer.equals(issuer)) {
                throw new RuntimeException("Invalid token issuer: " + issuer);
            }

            // Check expiration
            Date expiration = claimsSet.getExpirationTime();
            if (expiration != null && expiration.before(new Date())) {
                throw new RuntimeException("Token has expired");
            }

            return extractClaims(claimsSet);

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token: " + e.getMessage(), e);
        }
    }

    /**
     * Extract claims from JWT
     */
    private CognitoClaims extractClaims(JWTClaimsSet claimsSet) {
        // Extract standard claims
        String sub = claimsSet.getSubject();
        String email = (String) claimsSet.getClaim("email");
        String username = (String) claimsSet.getClaim("cognito:username");

        // If email is not in token (access token), use username
        if (email == null) {
            email = username;
        }

        // Extract groups (roles)
        @SuppressWarnings("unchecked")
        List<String> groups = (List<String>) claimsSet.getClaim("cognito:groups");

        // Determine token type
        String tokenUse = (String) claimsSet.getClaim("token_use");

        return CognitoClaims.builder()
                .sub(sub)
                .email(email)
                .username(username != null ? username : email)
                .groups(groups)
                .tokenUse(tokenUse)
                .expirationTime(claimsSet.getExpirationTime())
                .build();
    }

    /**
     * Claims extracted from Cognito JWT
     */
    @Getter
    @lombok.Builder
    public static class CognitoClaims {
        private final String sub;
        private final String email;
        private final String username;
        private final List<String> groups;
        private final String tokenUse;
        private final Date expirationTime;

        /**
         * Get roles formatted for Spring Security (ROLE_ prefix)
         */
        public List<String> getRoles() {
            if (groups == null || groups.isEmpty()) {
                return List.of("ROLE_USER"); // Default role
            }
            return groups.stream()
                    .map(group -> group.startsWith("ROLE_") ? group : "ROLE_" + group.toUpperCase())
                    .toList();
        }
    }
}
