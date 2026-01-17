package com.restaurant.profileservice.service;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.profileservice.dto.CreateProfileRequest;
import com.restaurant.profileservice.dto.ProfileDto;
import com.restaurant.profileservice.dto.UpdateProfileRequest;
import com.restaurant.profileservice.filter.ProfileFilter;
import com.restaurant.redismodule.exception.CacheException;

import java.util.List;

public interface ProfileService {
    /**
     * Create a new profile for a user
     */
    ProfileDto createProfile(String userId, CreateProfileRequest request) throws DataFactoryException;

    /**
     * Get profile by profile ID (used by admin)
     */
    ProfileDto getProfileById(Long id) throws CacheException, DataFactoryException;

    /**
     * Get profile by user ID (Cognito sub)
     */
    ProfileDto getProfileByUserId(String userId) throws CacheException, DataFactoryException;

    /**
     * Update profile by profile ID
     */
    ProfileDto updateProfile(Long id, UpdateProfileRequest request) throws DataFactoryException, CacheException;

    /**
     * Delete profile by profile ID
     */
    void deleteProfile(Long id) throws DataFactoryException, CacheException;

    /**
     * Get all profiles with filtering (admin only)
     */
    List<ProfileDto> getAllProfiles(ProfileFilter filter) throws CacheException, DataFactoryException;

    /**
     * Auto-create profile from user registration event
     * 
     * @param userId Cognito sub (UUID string)
     */
    void createProfileFromUserRegistration(String userId, String email, String fullName, String phone, String address)
            throws DataFactoryException;

    /**
     * Update loyalty points for a member
     * Adds points and handles max threshold (1000 points = free dessert, then reset
     * to 0)
     * 
     * @param userId      Cognito sub (UUID string)
     * @param pointsToAdd Points to add to current balance
     */
    void updateLoyaltyPoints(String userId, int pointsToAdd) throws DataFactoryException, CacheException;
}
