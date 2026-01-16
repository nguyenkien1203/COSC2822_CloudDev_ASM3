package com.restaurant.orderservice.dto;

import com.restaurant.orderservice.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusRequest {

    @NotNull(message = "New payment status is required")
    private PaymentStatus newPaymentStatus;
}
