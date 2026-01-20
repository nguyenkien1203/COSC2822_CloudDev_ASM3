package com.restaurant.profileservice.entity;

import com.restaurant.data.entity.IBaseEntity;
import com.restaurant.profileservice.enums.MembershipRank;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileEntity implements IBaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true)
    private String userId; // Cognito sub (UUID string)

    private String role; // User role from Cognito (ADMIN, USER, DRIVER, KITCHEN)

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    @Column(unique = true)
    private String email;

    private String address;

    @Column(name = "loyalty_points", nullable = false)
    @Builder.Default
    private Integer loyaltyPoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_rank", nullable = false)
    @Builder.Default
    private MembershipRank membershipRank = MembershipRank.SILVER;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
