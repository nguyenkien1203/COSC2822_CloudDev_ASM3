package com.restaurant.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request for admin to create dine-in order (without user authentication)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateDineInRequest {

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<CreateOrderItemRequest> items;

    private String notes;
}
