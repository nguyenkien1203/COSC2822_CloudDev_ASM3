package com.restaurant.orderservice.dto;

import com.restaurant.data.model.IBaseModel;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignDriverRequest implements IBaseModel<Long> {

    @NotNull(message = "Driver ID is required")
    private String driverId;

    @Override
    public Long getId() {
        return null; // This is a request DTO, not an entity with ID
    }
}
