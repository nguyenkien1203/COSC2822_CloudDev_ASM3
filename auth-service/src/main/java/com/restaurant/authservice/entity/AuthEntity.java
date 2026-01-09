package com.restaurant.authservice.entity;

import com.restaurant.data.entity.IBaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Data
@Table(name = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthEntity implements IBaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(name = "cognito_sub", unique = true)
    private String cognitoSub;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole role = UserRole.USER;

    public enum UserRole {
        USER, ADMIN, MANAGER
    }
}
