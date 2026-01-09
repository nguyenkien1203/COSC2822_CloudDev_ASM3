package com.restaurant.profileservice.filter;

import com.restaurant.data.model.IFilter;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileFilter implements IFilter {
    private String userId;  // Cognito sub (UUID string)
    private String email;
}
