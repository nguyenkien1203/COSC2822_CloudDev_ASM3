package com.restaurant.sqsmodule.event;

import lombok.*;
import lombok.experimental.SuperBuilder;
import com.restaurant.kafkamodule.event.BaseEvent;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
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
