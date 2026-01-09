package com.restaurant.profileservice.controller;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.profileservice.dto.CreateProfileRequest;
import com.restaurant.profileservice.dto.ProfileDto;
import com.restaurant.profileservice.dto.UpdateProfileRequest;
import com.restaurant.securitymodule.model.UserPrincipal;
import com.restaurant.profileservice.filter.ProfileFilter;
import com.restaurant.profileservice.service.ProfileProducerService;
import com.restaurant.profileservice.service.ProfileService;
import com.restaurant.redismodule.exception.CacheException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Autowired
    private final ProfileProducerService profileProducerService;

    /**
     * Get current user's profile
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileDto> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal)
            throws CacheException, DataFactoryException {

        String userId = principal.getUserId();
        log.info("GET /profiles/me - userId: {}", userId);
        ProfileDto profile = profileService.getProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Create profile for current user
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileDto> createProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProfileRequest request) throws DataFactoryException {

        String userId = principal.getUserId();
        log.info("POST /profiles - userId: {}", userId);
        ProfileDto profile = profileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    /**
     * Update current user's profile
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileDto> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) throws CacheException, DataFactoryException {

        String userId = principal.getUserId();
        log.info("PUT /profiles/me - userId: {}", userId);

        // First get the profile ID for this user
        ProfileDto existing = profileService.getProfileByUserId(userId);
        ProfileDto updated = profileService.updateProfile(existing.getId(), request);

        return ResponseEntity.ok(updated);
    }

    // ========== ADMIN ENDPOINTS ==========

    /**
     * Get profile by ID (Admin only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfileDto> getProfileById(@PathVariable Long id)
            throws CacheException, DataFactoryException {

        log.info("GET /profiles/{} - Admin access", id);
        ProfileDto profile = profileService.getProfileById(id);
        return ResponseEntity.ok(profile);
    }

    /**
     * Get all profiles (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProfileDto>> getAllProfiles(ProfileFilter filter)
            throws CacheException, DataFactoryException {

        log.info("GET /profiles - Admin access");
        List<ProfileDto> profiles = profileService.getAllProfiles(filter);
        return ResponseEntity.ok(profiles);
    }

    /**
     * Update any profile by ID (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfileDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request)
            throws CacheException, DataFactoryException {

        log.info("PUT /profiles/{} - Admin access", id);
        ProfileDto updated = profileService.updateProfile(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete profile (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) throws DataFactoryException, CacheException {

        log.info("DELETE /profiles/{} - Admin access", id);
        profileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get profile by userId (Admin only)
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfileDto> getProfileByUserId(@PathVariable String userId)
            throws CacheException, DataFactoryException {

        log.info("GET /profiles/user/{} - Admin access", userId);
        ProfileDto profile = profileService.getProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }
}