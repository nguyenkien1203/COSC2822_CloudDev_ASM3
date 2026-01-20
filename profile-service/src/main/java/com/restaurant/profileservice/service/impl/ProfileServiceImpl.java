package com.restaurant.profileservice.service.impl;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.profileservice.dto.CreateProfileRequest;
import com.restaurant.profileservice.dto.MembershipInfoDto;
import com.restaurant.profileservice.dto.ProfileDto;
import com.restaurant.profileservice.dto.UpdateProfileRequest;
import com.restaurant.profileservice.entity.ProfileEntity;
import com.restaurant.profileservice.enums.MembershipRank;
import com.restaurant.profileservice.event.DeleteProfileEvent;
import com.restaurant.profileservice.factory.ProfileFactory;
import com.restaurant.profileservice.filter.ProfileFilter;
import com.restaurant.profileservice.repository.ProfileRepository;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileFactory profileFactory;
    private final ProfileRepository profileRepository;

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
                .loyaltyPoints(0)
                .membershipRank(MembershipRank.SILVER)
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
        return profileDto;
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
    public void createProfileFromUserRegistration(String userId, String email, String fullName, String phone,
            String address) throws DataFactoryException {
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
                .loyaltyPoints(0)
                .membershipRank(MembershipRank.SILVER)
                .build();
        profileFactory.create(profileDto);
        log.info("Profile auto-created for email: {}", email);
    }

    @Override
    @Transactional
    public void updateLoyaltyPoints(String userId, int pointsToAdd) throws DataFactoryException, CacheException {
        log.info("Updating loyalty points for userId: {}, points to add: {}", userId, pointsToAdd);

        // Get the profile entity for this user
        Optional<ProfileEntity> profileEntityOpt = profileRepository.findByUserId(userId);
        if (profileEntityOpt.isEmpty()) {
            log.warn("Profile not found for userId: {}, skipping loyalty points update", userId);
            return;
        }

        ProfileEntity profileEntity = profileEntityOpt.get();

        // Get current loyalty points (default to 0 if null)
        int currentPoints = profileEntity.getLoyaltyPoints() != null ? profileEntity.getLoyaltyPoints() : 0;

        // Get current membership rank (default to SILVER if null)
        MembershipRank currentRank = profileEntity.getMembershipRank() != null
                ? profileEntity.getMembershipRank()
                : MembershipRank.SILVER;

        // Calculate new points
        int newPoints = currentPoints + pointsToAdd;

        // Get threshold for current rank
        int threshold = getThresholdForRank(currentRank);

        // Check if threshold is reached based on current rank
        if (newPoints >= threshold) {
            if (currentRank == MembershipRank.VIP) {
                // For VIP, cap at max (10000) but don't reset
                if (newPoints > VIP_MAX_POINTS) {
                    profileEntity.setLoyaltyPoints(VIP_MAX_POINTS);
                    log.info("VIP member reached max points cap for userId: {} (current: {}, adding: {}, new: {}). " +
                            "Capping points at {}.",
                            userId, currentPoints, pointsToAdd, newPoints, VIP_MAX_POINTS);
                } else {
                    profileEntity.setLoyaltyPoints(newPoints);
                    log.info("Updated loyalty points for VIP member userId: {} from {} to {}",
                            userId, currentPoints, newPoints);
                }
            } else if (currentRank == MembershipRank.PLATINUM) {
                // For PLATINUM, upgrade to VIP when reaching 5000 points
                MembershipRank newRank = upgradeMembershipRank(currentRank);
                log.info("Loyalty points threshold reached for userId: {} (current: {}, adding: {}, new: {}). " +
                        "Upgrading membership rank from {} to {} and resetting points to 0.",
                        userId, currentPoints, pointsToAdd, newPoints, currentRank, newRank);
                profileEntity.setLoyaltyPoints(0);
                profileEntity.setMembershipRank(newRank);
                log.info("Membership rank upgraded for user: {} from {} to {}", userId, currentRank, newRank);
            } else {
                // Upgrade to next rank (SILVER -> GOLD or GOLD -> PLATINUM)
                MembershipRank newRank = upgradeMembershipRank(currentRank);
                log.info("Loyalty points threshold reached for userId: {} (current: {}, adding: {}, new: {}). " +
                        "Upgrading membership rank from {} to {} and resetting points to 0.",
                        userId, currentPoints, pointsToAdd, newPoints, currentRank, newRank);
                profileEntity.setLoyaltyPoints(0);
                profileEntity.setMembershipRank(newRank);
                log.info("Membership rank upgraded for user: {} from {} to {}", userId, currentRank, newRank);
            }
        } else {
            // Update with new points (but cap VIP at max)
            if (currentRank == MembershipRank.VIP && newPoints > VIP_MAX_POINTS) {
                profileEntity.setLoyaltyPoints(VIP_MAX_POINTS);
                log.info("VIP member points capped at max for userId: {} (current: {}, adding: {}, capped at: {})",
                        userId, currentPoints, pointsToAdd, VIP_MAX_POINTS);
            } else {
                profileEntity.setLoyaltyPoints(newPoints);
                log.info("Updated loyalty points for userId: {} from {} to {} (rank: {})",
                        userId, currentPoints, newPoints, currentRank);
            }
        }

        // Save the updated profile
        // Create a DTO with the updates
        ProfileDto updateDto = ProfileDto.builder()
                .id(profileEntity.getId())
                .loyaltyPoints(profileEntity.getLoyaltyPoints())
                .membershipRank(profileEntity.getMembershipRank())
                .build();

        // Use factory to update, which handles caching
        log.info("Calling profileFactory.update for profile id: {} with loyaltyPoints: {}",
                profileEntity.getId(), profileEntity.getLoyaltyPoints());
        profileFactory.update(updateDto);
        log.info("Successfully updated loyalty points for userId: {}", userId);
    }

    /**
     * Get the points threshold for a given membership rank
     * SILVER -> GOLD: 800 points
     * GOLD -> PLATINUM: 1200 points (additional)
     * PLATINUM -> VIP: 5000 points
     * VIP: 10000 points (max)
     */
    private int getThresholdForRank(MembershipRank rank) {
        return switch (rank) {
            case SILVER -> SILVER_TO_GOLD_THRESHOLD; // 800 points
            case GOLD -> GOLD_TO_PLATINUM_THRESHOLD; // 1200 points
            case PLATINUM -> PLATINUM_TO_VIP_THRESHOLD; // 3000 points
            case VIP -> VIP_MAX_POINTS; // 10000 points (cap, don't reset)
        };
    }

    /**
     * Upgrade membership rank to the next tier
     * SILVER -> GOLD -> PLATINUM -> VIP (stays at VIP if already highest)
     */
    private MembershipRank upgradeMembershipRank(MembershipRank currentRank) {
        return switch (currentRank) {
            case SILVER -> MembershipRank.GOLD;
            case GOLD -> MembershipRank.PLATINUM;
            case PLATINUM -> MembershipRank.VIP;
            case VIP -> MembershipRank.VIP; // Already at highest rank
        };
    }

    // Points thresholds for each rank
    private static final int SILVER_TO_GOLD_THRESHOLD = 800; // Points needed to upgrade from SILVER to GOLD
    private static final int GOLD_TO_PLATINUM_THRESHOLD = 1200; // Additional points needed to upgrade from GOLD to
    private static final int PLATINUM_TO_VIP_THRESHOLD = 3000; // Points needed to upgrade from PLATINUM to VIP
    private static final int VIP_MAX_POINTS = 10000; // Maximum points for VIP rank (cap, don't reset)

    @Override
    public MembershipInfoDto getMembershipInfo(String userId) throws CacheException, DataFactoryException {
        log.info("Getting membership info for userId: {}", userId);

        Optional<ProfileEntity> profileEntityOpt = profileRepository.findByUserId(userId);
        if (profileEntityOpt.isEmpty()) {
            log.warn("Profile not found for userId: {}, returning default membership info", userId);
            // Return default (SILVER with 5% discount) for users without profile
            return MembershipInfoDto.builder()
                    .membershipRank("SILVER")
                    .discountPercentage(5)
                    .loyaltyPoints(0)
                    .build();
        }

        ProfileEntity profile = profileEntityOpt.get();
        MembershipRank rank = profile.getMembershipRank() != null
                ? profile.getMembershipRank()
                : MembershipRank.SILVER;
        int loyaltyPoints = profile.getLoyaltyPoints() != null
                ? profile.getLoyaltyPoints()
                : 0;

        log.info("Membership info for userId: {} - rank: {}, discount: {}%, points: {}",
                userId, rank.name(), rank.getDiscountPercentage(), loyaltyPoints);

        return MembershipInfoDto.builder()
                .membershipRank(rank.name())
                .discountPercentage(rank.getDiscountPercentage())
                .loyaltyPoints(loyaltyPoints)
                .build();
    }
}
