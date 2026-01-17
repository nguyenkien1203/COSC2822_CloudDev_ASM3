package com.restaurant.profileservice.factory;

import com.restaurant.data.model.IFilter;
import com.restaurant.factorymodule.BaseCrudFactory;
import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.profileservice.dto.ProfileDto;
import com.restaurant.profileservice.entity.ProfileEntity;
import com.restaurant.profileservice.filter.ProfileFilter;
import com.restaurant.profileservice.repository.ProfileRepository;
import com.restaurant.redismodule.factory.CacheConfigFactory;
import com.restaurant.redismodule.service.ICacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class ProfileFactory extends BaseCrudFactory<Long, ProfileDto, Long, ProfileEntity, ProfileRepository> {
    protected ProfileFactory(ICacheService iCacheService, ProfileRepository crudRepository) {
        super(iCacheService, crudRepository);
    }

    @Override
    protected ProfileDto convertToModel(ProfileEntity entity) {
        return ProfileDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .fullName(entity.getFullName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .loyaltyPoints(entity.getLoyaltyPoints())
                .membershipRank(entity.getMembershipRank())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    protected ProfileEntity createConvertToEntity(ProfileDto model) {
        return ProfileEntity.builder()
                .userId(model.getUserId())
                .fullName(model.getFullName())
                .phone(model.getPhone())
                .email(model.getEmail())
                .address(model.getAddress())
                .loyaltyPoints(model.getLoyaltyPoints() != null ? model.getLoyaltyPoints() : 0)
                .membershipRank(model.getMembershipRank() != null ? model.getMembershipRank()
                        : com.restaurant.profileservice.enums.MembershipRank.SILVER)
                .build();
    }

    @Override
    protected ProfileEntity updateConvertToEntity(ProfileDto model, ProfileEntity oldEntity) {
        if (model == null || oldEntity == null) {
            return oldEntity;
        }

        // Update only non-null fields
        if (model.getFullName() != null) {
            oldEntity.setFullName(model.getFullName());
        }
        if (model.getPhone() != null) {
            oldEntity.setPhone(model.getPhone());
        }
        if (model.getEmail() != null) {
            oldEntity.setEmail(model.getEmail());
        }
        if (model.getAddress() != null) {
            oldEntity.setAddress(model.getAddress());
        }
        if (model.getLoyaltyPoints() != null) {
            oldEntity.setLoyaltyPoints(model.getLoyaltyPoints());
        }
        if (model.getMembershipRank() != null) {
            oldEntity.setMembershipRank(model.getMembershipRank());
        }

        return oldEntity;
    }

    @Override
    public CacheConfigFactory<ProfileDto> cacheFactory() {
        return new CacheConfigFactory<ProfileDto>() {
            @Override
            public Class<ProfileDto> getModelClass() {
                return ProfileDto.class;
            }

            @Override
            public Duration singleTtl() {
                return Duration.ofMinutes(30); // Cache single profile for 30 minutes
            }

            @Override
            public Duration cacheListTtl() {
                return Duration.ofMinutes(15); // Cache list for 15 minutes
            }
        };
    }

    @Override
    protected <F extends IFilter> Optional<ProfileEntity> getEntity(Long id, F filter) throws DataFactoryException {
        if (id != null) {
            return crudRepository.findById(id);
        }

        if (filter instanceof ProfileFilter profileFilter) {
            if (profileFilter.getUserId() != null) {
                return crudRepository.findByUserId(profileFilter.getUserId());
            }
            if (profileFilter.getEmail() != null) {
                return crudRepository.findByEmail(profileFilter.getEmail());
            }
        }

        throw new DataFactoryException("Please provide id or filter with userId/email");
    }

    @Override
    public <F extends IFilter> boolean exists(Long id, F filter) throws DataFactoryException {
        if (id != null) {
            return crudRepository.existsById(id);
        }

        if (filter instanceof ProfileFilter profileFilter) {
            if (profileFilter.getUserId() != null) {
                return crudRepository.existsByUserId(profileFilter.getUserId());
            }
            if (profileFilter.getEmail() != null) {
                return crudRepository.findByEmail(profileFilter.getEmail()).isPresent();
            }
        }

        throw new DataFactoryException("Please provide id or filter with userId/email");
    }

}
