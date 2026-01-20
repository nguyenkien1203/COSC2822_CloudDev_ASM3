package com.restaurant.reservationservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for profile information from profile-service
 * Uses @JsonIgnoreProperties to ignore unknown fields from the response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileInfoDto {
    private Long id;
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
}
