package com.restaurant.kafkamodule.exception;

/**
 * Custom exception for Kafka operations
 */
public class KafkaException extends RuntimeException {
    
    public KafkaException(String message) {
        super(message);
    }
    
    public KafkaException(String message, Throwable cause) {
        super(message, cause);
    }
}

