package com.restaurant.profileservice.service.impl;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.profileservice.dto.CreateProfileRequest;
import com.restaurant.profileservice.dto.ProfileDto;
import com.restaurant.profileservice.dto.UpdateProfileRequest;
import com.restaurant.profileservice.event.DeleteProfileEvent;
import com.restaurant.profileservice.factory.ProfileFactory;
import com.restaurant.profileservice.filter.ProfileFilter;
import com.restaurant.profileservice.service.ProfileProducerService;
import com.restaurant.profileservice.service.ProfileService;
import com.restaurant.redismodule.exception.CacheException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileFactory profileFactory;

    @Autowired
    private final ProfileProducerService profileProducerService;

    @Value("${spring.application.name:profile-service}")
    private String serviceName;

    @Override
    @Transactional
    public ProfileDto createProfile(String userId, CreateProfileRequest request) throws DataFactoryException {
        log.info("Creating profile for userId: {}", userId);
        ProfileFilter profileFilter = ProfileFilter.builder()
                .userId(userId)
                .build();
        if (profileFactory.exists(null, profileFilter)) {
            log.error("Profile already exists for userId: {}", userId);
            throw new DataFactoryException("Profile already exists for userId: " + userId);
        }
        ProfileDto profileDto = ProfileDto.builder()
                .userId(userId)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        return profileFactory.create(profileDto);
    }

    @Override
    public ProfileDto getProfileById(Long id) throws CacheException, DataFactoryException {
        log.info("Getting profile by ID: {}", id);
        return profileFactory.getModel(id, null);
    }

    @Override
    public ProfileDto getProfileByUserId(String userId) throws CacheException, DataFactoryException {
        log.info("Getting profile by userId: {}", userId);
        ProfileFilter filter = ProfileFilter.builder()
                .userId(userId)
                .build();
        ProfileDto profileDto = profileFactory.getModel(filter);
        return profileFactory.getModel(profileDto.getId());
    }

    @Override
    @Transactional
    public ProfileDto updateProfile(Long id, UpdateProfileRequest request) throws DataFactoryException, CacheException {
        log.info("Updating profile with id: {}", id);
        if (!profileFactory.exists(id, null)) {
            log.error("Profile not found with id: {}", id);
            throw new DataFactoryException("Profile not found with id: " + id);
        }
        ProfileDto existingProfile = profileFactory.getModel(id);
        // Update only non-null fields
        if (request.getFullName() != null) {
            existingProfile.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            existingProfile.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            existingProfile.setAddress(request.getAddress());
        }
        log.info("Profile updated: {}", id);
        return profileFactory.update(existingProfile);
    }

    @Override
    @Transactional
    public void deleteProfile(Long id) throws DataFactoryException, CacheException {

        String userId = profileFactory.getModel(id).getUserId();

        log.info("Deleting profile with id: {}", id);
        if (!profileFactory.exists(id, null)) {
            log.error("Profile not found with id: {}", id);
            throw new DataFactoryException("Profile not found with id: " + id);
        }
        profileFactory.delete(id);
        profileProducerService.publishDeleteProfileEvent(DeleteProfileEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PROFILE_DELETE")
                .timestamp(LocalDateTime.now())
                .source(serviceName)
                .version("1.0")
                .userId(userId)
                .build());
        log.info("Profile deleted: {}", id);
    }

    @Override
    public List<ProfileDto> getAllProfiles(ProfileFilter filter) throws CacheException, DataFactoryException {
        log.info("Getting all profiles with filter");
        return profileFactory.getList(filter);
    }

    @Override
    @Transactional
    public void createProfileFromUserRegistration(String userId, String email, String fullName, String phone, String address) throws DataFactoryException {
        log.info("Auto-creating profile from user registration - userId: {}, email: {}", userId, email);

        // Check if profile already exists by email (in case of duplicate events)
        ProfileFilter filter = ProfileFilter.builder()
                .email(email)
                .build();
        if (profileFactory.exists(null, filter)) {
            log.warn("Profile already exists for email: {}, skipping creation", email);
            return;
        }
        
        ProfileDto profileDto = ProfileDto.builder()
                .userId(userId)
                .email(email)
                .fullName(fullName)
                .phone(phone)
                .address(address)
                .build();
        profileFactory.create(profileDto);
        log.info("Profile auto-created for email: {}", email);
    }
}
