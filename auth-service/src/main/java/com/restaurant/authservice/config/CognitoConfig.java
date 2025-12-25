package com.restaurant.authservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * AWS Cognito client configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CognitoConfig {

    private final CognitoProperties cognitoProperties;

    /**
     * Create Cognito Identity Provider client
     * Uses default credentials provider chain (env vars, instance profile, etc.)
     */
    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        log.info("Initializing Cognito client for region: {}, userPoolId: {}",
                cognitoProperties.getRegion(), cognitoProperties.getUserPoolId());

        return CognitoIdentityProviderClient.builder()
                .region(Region.of(cognitoProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
