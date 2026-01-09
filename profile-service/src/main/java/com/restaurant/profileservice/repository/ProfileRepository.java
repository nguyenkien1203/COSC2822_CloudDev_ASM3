package com.restaurant.profileservice.repository;

import com.restaurant.profileservice.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    Optional<ProfileEntity> findByUserId(String userId);
    Optional<ProfileEntity> findByEmail(String email);
    boolean existsByUserId(String userId);
}
