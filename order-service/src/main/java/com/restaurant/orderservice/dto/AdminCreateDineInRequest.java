package com.restaurant.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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


    private Long tableId;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<CreateOrderItemRequest> items;

    private String notes;

    // Optional: link to an existing reservation
    @NotNull(message = "Reservation ID is required")
    private Long reservationId;
}
