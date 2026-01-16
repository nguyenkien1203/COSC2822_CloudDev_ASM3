package com.restaurant.orderservice.dto;

import com.restaurant.orderservice.enums.OrderType;
import com.restaurant.orderservice.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    // Order type - auto-set to PRE_ORDER for createPreOrder endpoint, required for
    // regular orders
    private OrderType orderType;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<CreateOrderItemRequest> items;

    private PaymentMethod paymentMethod;

    // For delivery orders
    private String deliveryAddress;

    // For pre-orders linked to reservation
    private Long reservationId;

    // Special instructions
    private String notes;

    // Estimated pickup time (for TAKEAWAY orders)
    private LocalDateTime estimatedPickupTime;

    // Guest information (for guest orders)
    private String guestEmail;
    private String guestPhone;
    private String guestName;
}
