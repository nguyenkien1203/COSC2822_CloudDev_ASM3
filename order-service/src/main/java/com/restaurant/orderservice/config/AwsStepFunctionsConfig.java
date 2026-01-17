package com.restaurant.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;

/**
 * AWS SDK configuration for Step Functions client
 */
@Configuration
public class AwsStepFunctionsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public SfnClient sfnClient() {
        return SfnClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
