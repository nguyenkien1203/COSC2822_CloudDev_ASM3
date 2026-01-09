package com.restaurant.authservice.service;

import com.restaurant.authservice.dto.AuthDto;
import com.restaurant.authservice.dto.AuthFilter;
import com.restaurant.authservice.dto.RegisterDto;
import com.restaurant.authservice.entity.AuthEntity;
import com.restaurant.authservice.event.LoginEvent;
import com.restaurant.sqsmodule.event.RegisterEvent;
import com.restaurant.authservice.event.TokenRefreshEvent;
import com.restaurant.authservice.event.UserLogoutEvent;
import com.restaurant.authservice.factory.AuthFactory;
import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.redismodule.exception.CacheException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthFactory authFactory;
    private final PasswordEncoder passwordEncoder;
    private final AuthProducerService kafkaProducerService;

    @Value("${spring.application.name:auth-service}")
    private String serviceName;

    /**
     * Register a new user and publish registration event to Kafka
     */
    public AuthDto register(RegisterDto registerDto) throws DataFactoryException {
        log.info("Registering new user with email: {}", registerDto.getEmail());

        // Check if user already exists
        AuthFilter authFilter = AuthFilter.builder()
                .email(registerDto.getEmail())
                .build();
        if (authFactory.exists(null, authFilter)) {
            throw new DataFactoryException("User with email " + registerDto.getEmail() + " already exists");
        }

        // Encode password
        String encodedPassword = passwordEncoder.encode(registerDto.getPassword());

        // Create user
        AuthDto newUser = AuthDto.builder()
                .email(registerDto.getEmail())
                .password(encodedPassword)
                .role(AuthEntity.UserRole.USER) // Default role
                .isActive(true)
                .build();

        AuthDto createdUser = authFactory.create(newUser);

        // Publish user registered event to Kafka
        kafkaProducerService.publishUserRegisteredEvent(
                RegisterEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("USER_REGISTERED")
                        .timestamp(LocalDateTime.now())
                        .source(serviceName)
                        .version("1.0")
                        .id(createdUser.getId())
                        .email(createdUser.getEmail())
                        .role(createdUser.getRole().name())
                        .fullName(registerDto.getFullName())
                        .phone(registerDto.getPhone())
                        .address(registerDto.getAddress())
                        .build());

        return createdUser;
    }

    /**
     * Authenticate user and publish login event to Kafka
     */
    public AuthDto login(String email, String password) throws DataFactoryException, CacheException {
        log.info("Login attempt for email: {}", email);

        // Find user by email
        AuthFilter authFilter = AuthFilter.builder()
                .email(email)
                .build();

        AuthDto authDto = authFactory.getModel(authFilter);
        if (authDto == null) {
            throw new DataFactoryException("User not found with email: " + email);
        }

        // Verify password
        if (!passwordEncoder.matches(password, authDto.getPassword())) {
            throw new DataFactoryException("Invalid email or password");
        }

        // Check if user is active
        if (!authDto.getIsActive()) {
            throw new DataFactoryException("User account is not active");
        }

        // Publish login event to Kafka
        kafkaProducerService.publishUserLoginEvent(
                LoginEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("USER_LOGIN")
                        .timestamp(LocalDateTime.now())
                        .source(serviceName)
                        .version("1.0")
                        .id(authDto.getId())
                        .email(authDto.getEmail())
                        .build());

        // Don't return password in the response
        authDto.setPassword(null);

        return authDto;
    }

    /**
     * Logout user and publish logout event to Kafka
     */
    public void logout(String email) {
        try {
            log.info("Logout request for email: {}", email);

            // Get user details
            AuthDto user = getUserByEmail(email);

            // Publish logout event to Kafka
            kafkaProducerService.publishUserLogoutEvent(
                    UserLogoutEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("USER_LOGOUT")
                            .timestamp(LocalDateTime.now())
                            .source(serviceName)
                            .version("1.0")
                            .userId(user.getId())
                            .email(user.getEmail())
                            .build());
        } catch (Exception e) {
            log.error("Error publishing logout event for email: {}", email, e);
            // Don't fail the logout if Kafka publish fails
        }
    }

    /**
     * Publish token refresh event to Kafka
     */
    public void publishTokenRefreshEvent(Long userId, String email) {
        try {
            log.info("Publishing token refresh event for userId: {}", userId);

            kafkaProducerService.publishTokenRefreshedEvent(
                    TokenRefreshEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("TOKEN_REFRESHED")
                            .timestamp(LocalDateTime.now())
                            .source(serviceName)
                            .version("1.0")
                            .id(userId)
                            .email(email)
                            .build());
        } catch (Exception e) {
            log.error("Error publishing token refresh event for userId: {}", userId, e);
            // Don't fail the refresh if Kafka publish fails
        }
    }

    public AuthDto getUserByEmail(String email) throws DataFactoryException, CacheException {
        log.info("Fetching user by email: {}", email);

        // Build filter to find user by email
        AuthFilter authFilter = AuthFilter.builder()
                .email(email)
                .build();

        // Get user from database via factory (uses cache if available)
        AuthDto authDto = authFactory.getModel(authFilter);

        if (authDto == null) {
            throw new DataFactoryException("User not found with email: " + email);
        }

        // Don't return password
        authDto.setPassword(null);

        return authDto;
    }

    public void deleteAuthRecord(Long userId) throws DataFactoryException, CacheException {
        log.info("Delete authentication record of user: {}", userId);

        if(userId == null) {
            throw new DataFactoryException("Userid is null");
        }

        if(!authFactory.exist(userId)){
            throw new DataFactoryException("Record does not exist");
        }
        authFactory.delete(userId);
    }

}
