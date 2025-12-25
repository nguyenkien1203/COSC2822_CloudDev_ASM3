package com.restaurant.authservice.event;

import com.restaurant.kafkamodule.event.BaseEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RegisterEvent extends BaseEvent {

    private String email;

    private String role;

    private boolean isActive;

    private Long id;

    private String fullName;

    private String phone;

    private String address;

    /**
     * Cognito user sub (unique identifier)
     */
    private String cognitoSub;

}
