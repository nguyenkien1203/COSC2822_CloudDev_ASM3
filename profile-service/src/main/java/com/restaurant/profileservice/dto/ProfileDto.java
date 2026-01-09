package com.restaurant.profileservice.dto;

import com.restaurant.data.model.IBaseModel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProfileDto implements IBaseModel<Long> {
    private Long id;

    private String userId;  // Cognito sub (UUID string)

    private String fullName;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private String address;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
