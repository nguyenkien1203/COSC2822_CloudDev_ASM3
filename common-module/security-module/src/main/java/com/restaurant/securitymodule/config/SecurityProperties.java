package com.restaurant.securitymodule.config;

import com.restaurant.securitymodule.enums.SecurityType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Security module configuration properties
 * Configurable via application.yml under 'restaurant.security' prefix
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "restaurant.security")
public class SecurityProperties {

    /**
     * JWT configuration
     */
    private Jwt jwt = new Jwt();

    /**
     * Cognito configuration
     */
    private Cognito cognito = new Cognito();

    /**
     * Endpoint security mappings
     */
    private List<EndpointSecurity> endpoints = new ArrayList<>();

    /**
     * Default security type for unmapped endpoints
     */
    private SecurityType defaultType = SecurityType.JWT;

    @Data
    public static class Jwt {
        /**
         * Cookie name containing JWT token
         */
        private String cookieName = "auth_token";
    }

    @Data
    public static class Cognito {
        /**
         * AWS Region where Cognito User Pool is located
         */
        private String region;

        /**
         * Cognito User Pool ID
         */
        private String userPoolId;

        /**
         * Get the JWKS URL for this Cognito User Pool
         */
        public String getJwksUrl() {
            if (region == null || userPoolId == null) {
                return null;
            }
            return String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
                    region, userPoolId);
        }

        /**
         * Get the issuer URL for this Cognito User Pool
         */
        public String getIssuerUrl() {
            if (region == null || userPoolId == null) {
                return null;
            }
            return String.format("https://cognito-idp.%s.amazonaws.com/%s",
                    region, userPoolId);
        }
    }

    @Data
    public static class EndpointSecurity {
        /**
         * URL path pattern (supports Ant-style patterns)
         */
        private String path;

        /**
         * Security type for this path
         */
        private SecurityType type;
    }
}
