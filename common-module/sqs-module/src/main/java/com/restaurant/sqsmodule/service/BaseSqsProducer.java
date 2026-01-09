package com.restaurant.sqsmodule.service;

import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Base SQS Producer Service
 * Provides common functionality for sending events to SQS queues
 * All microservices can extend this or use it directly
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaseSqsProducer implements IBaseSqsProducer {

    private final SqsTemplate sqsTemplate;

    /**
     * Send event to SQS queue
     * 
     * @param queueName The queue name
     * @param event     The event object to send
     */
    @Override
    public void sendEvent(String queueName, Object event) {
        try {
            sqsTemplate.send(to -> to
                    .queue(queueName)
                    .payload(event));

            log.info("Sent event to queue={}", queueName);
            log.debug("Event payload: {}", event);
        } catch (Exception e) {
            log.error("Error sending event to queue={}", queueName, e);
        }
    }

    /**
     * Send event synchronously (blocks until confirmation)
     * 
     * @param queueName The queue name
     * @param event     The event object to send
     * @return SendResult containing metadata
     */
    @Override
    public SendResult<Object> sendEventSync(String queueName, Object event) {
        try {
            SendResult<Object> result = sqsTemplate.send(to -> to
                    .queue(queueName)
                    .payload(event));

            log.info("Sent event synchronously to queue={}, messageId={}",
                    queueName,
                    result.messageId());
            return result;
        } catch (Exception e) {
            log.error("Failed to send event synchronously to queue={}", queueName, e);
            throw new RuntimeException("Failed to send event synchronously", e);
        }
    }
}
