package com.restaurant.profileservice.service.impl;

import com.restaurant.sqsmodule.config.SqsQueueConfig;
import com.restaurant.sqsmodule.service.BaseSqsProducer;
import com.restaurant.profileservice.event.DeleteProfileEvent;
import com.restaurant.profileservice.service.ProfileProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProfileProducerServiceImpl implements ProfileProducerService {

    private final BaseSqsProducer sqsProducer;

    public ProfileProducerServiceImpl(BaseSqsProducer sqsProducer) {
        this.sqsProducer = sqsProducer;
    }

    @Override
    public void publishDeleteProfileEvent(DeleteProfileEvent deleteProfileEvent) {
        log.debug("Publishing user registered event for userId: {}", deleteProfileEvent.getUserId());
        sqsProducer.sendEvent(
                SqsQueueConfig.PROFILE_DELETED_QUEUE,
                deleteProfileEvent);
    }
}
