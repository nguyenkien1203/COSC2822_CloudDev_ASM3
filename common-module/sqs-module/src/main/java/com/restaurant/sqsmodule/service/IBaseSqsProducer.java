package com.restaurant.sqsmodule.service;

import io.awspring.cloud.sqs.operations.SendResult;
import java.util.UUID;

public interface IBaseSqsProducer {

    /**
     * Send event to SQS queue
     * 
     * @param queueName The queue name
     * @param event     The event object to send
     */
    void sendEvent(String queueName, Object event);

    /**
     * Send event to SQS queue synchronously
     * 
     * @param queueName The queue name
     * @param event     The event object to send
     * @return SendResult containing metadata
     */
    SendResult<Object> sendEventSync(String queueName, Object event);
}
