package com.restaurant.profileservice.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * SQS listener configuration for profile-service.
 * Configures message converter to handle String payloads directly,
 * allowing manual JSON deserialization and avoiding JavaType header issues.
 */
@Configuration
public class SqsConfig {

    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient) {

        // Use default SQS message converter with payloads as String
        SqsMessagingMessageConverter messageConverter = new SqsMessagingMessageConverter();
        // Do not try to resolve type from headers - just pass the raw payload
        messageConverter.setPayloadTypeMapper(message -> String.class);

        return SqsMessageListenerContainerFactory.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> options.messageConverter(messageConverter))
                .build();
    }
}
