package com.restaurant.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AWS Cognito
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aws.cognito")
public class CognitoProperties {

    /**
     * AWS Region where Cognito User Pool is located
     */
    private String region;

    /**
     * Cognito User Pool ID
     */
    private String userPoolId;

    /**
     * Cognito App Client ID
     */
    private String clientId;

    /**
     * Cognito App Client Secret (optional, depends on client configuration)
     */
    private String clientSecret;

    /**
     * Get the JWKS URL for this Cognito User Pool
     */
    public String getJwksUrl() {
        return String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
                region, userPoolId);
    }

    /**
     * Get the issuer URL for this Cognito User Pool
     */
    public String getIssuerUrl() {
        return String.format("https://cognito-idp.%s.amazonaws.com/%s",
                region, userPoolId);
    }
}
