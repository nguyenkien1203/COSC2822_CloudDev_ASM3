package com.restaurant.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.orderservice.dto.DiscountResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartSyncExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartSyncExecutionResponse;
import software.amazon.awssdk.services.sfn.model.SyncExecutionStatus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for calling Step Functions discount calculator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StepFunctionsDiscountService {

    private final SfnClient sfnClient;
    private final ObjectMapper objectMapper;

    @Value("${step-functions.discount-calculator-arn:}")
    private String stateMachineArn;

    /**
     * Calculate discount using Step Functions state machine
     *
     * @param userId   User's Cognito sub
     * @param subtotal Order subtotal before discount
     * @return DiscountResult with discount details
     */
    public DiscountResult calculateDiscount(String userId, BigDecimal subtotal) {
        if (stateMachineArn == null || stateMachineArn.isEmpty()) {
            log.warn("Step Functions ARN not configured, returning no discount");
            return DiscountResult.noDiscount();
        }

        try {
            // Prepare input for Step Functions
            Map<String, Object> input = new HashMap<>();
            input.put("userId", userId);
            input.put("subtotal", subtotal.doubleValue());

            String inputJson = objectMapper.writeValueAsString(input);
            log.info("Starting Step Functions execution for user: {}, subtotal: {}", userId, subtotal);

            // Start synchronous execution (Express workflow)
            StartSyncExecutionRequest request = StartSyncExecutionRequest.builder()
                    .stateMachineArn(stateMachineArn)
                    .input(inputJson)
                    .build();

            StartSyncExecutionResponse response = sfnClient.startSyncExecution(request);

            if (response.status() == SyncExecutionStatus.SUCCEEDED) {
                String outputJson = response.output();
                log.info("Step Functions execution succeeded: {}", outputJson);

                // Parse the response
                Map<String, Object> result = objectMapper.readValue(outputJson, Map.class);

                return DiscountResult.builder()
                        .membershipRank((String) result.get("membershipRank"))
                        .discountPercentage(((Number) result.get("discountPercentage")).intValue())
                        .discountAmount(BigDecimal.valueOf(((Number) result.get("discountAmount")).doubleValue()))
                        .loyaltyPoints(((Number) result.getOrDefault("loyaltyPoints", 0)).intValue())
                        .build();
            } else {
                log.error("Step Functions execution failed: status={}, error={}",
                        response.status(), response.error());
                return DiscountResult.noDiscount();
            }

        } catch (Exception e) {
            log.error("Error calling Step Functions discount calculator: {}", e.getMessage(), e);
            return DiscountResult.noDiscount();
        }
    }
}
