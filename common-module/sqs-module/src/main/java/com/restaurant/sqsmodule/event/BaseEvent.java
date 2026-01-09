package com.restaurant.kafkamodule.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Base Event class for all Kafka events
 * All event types should extend this class
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    
    /**
     * Unique event identifier
     */
    private String eventId;
    
    /**
     * Event type identifier
     */
    private String eventType;
    
    /**
     * Timestamp when the event was created
     */
    private LocalDateTime timestamp;
    
    /**
     * Source service that generated the event
     */
    private String source;
    
    /**
     * Event version for compatibility tracking
     */
    private String version;
}

