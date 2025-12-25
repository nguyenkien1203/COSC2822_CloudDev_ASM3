package com.restaurant.authservice.service;

import com.restaurant.authservice.config.CognitoProperties;
import com.restaurant.authservice.dto.CognitoAuthResponse;
import com.restaurant.authservice.dto.NewPasswordRequest;
import com.restaurant.authservice.dto.RegisterDto;
import com.restaurant.authservice.event.LoginEvent;
import com.restaurant.authservice.event.RegisterEvent;
import com.restaurant.authservice.event.TokenRefreshEvent;
import com.restaurant.authservice.event.UserLogoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for AWS Cognito authentication operations
 * Handles login, registration, token refresh, and password management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoAuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final CognitoProperties cognitoProperties;
    private final AuthProducerService kafkaProducerService;

    @Value("${spring.application.name:auth-service}")
    private String serviceName;

    /**
     * Authenticate user with Cognito using USER_PASSWORD_AUTH flow
     *
     * @param email    User's email
     * @param password User's password
     * @return CognitoAuthResponse with tokens or challenge info
     */
    public CognitoAuthResponse login(String email, String password) {
        log.info("Cognito login attempt for email: {}", email);

        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);

            // Add secret hash if client secret is configured
            if (cognitoProperties.getClientSecret() != null && !cognitoProperties.getClientSecret().isEmpty()) {
                authParams.put("SECRET_HASH", calculateSecretHash(email));
            }

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .clientId(cognitoProperties.getClientId())
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            // Check if there's a challenge (e.g., NEW_PASSWORD_REQUIRED)
            if (authResponse.challengeName() != null) {
                log.info("Cognito challenge received: {}", authResponse.challengeName());
                return handleChallenge(authResponse, email);
            }

            // Successful authentication
            AuthenticationResultType result = authResponse.authenticationResult();

            // Publish login event to Kafka
            publishLoginEvent(email);

            return CognitoAuthResponse.builder()
                    .accessToken(result.accessToken())
                    .idToken(result.idToken())
                    .refreshToken(result.refreshToken())
                    .expiresIn(result.expiresIn())
                    .tokenType(result.tokenType())
                    .email(email)
                    .build();

        } catch (NotAuthorizedException e) {
            log.warn("Cognito authentication failed for email: {} - {}", email, e.getMessage());
            throw new RuntimeException("Invalid email or password");
        } catch (UserNotFoundException e) {
            log.warn("User not found in Cognito: {}", email);
            throw new RuntimeException("User not found");
        } catch (UserNotConfirmedException e) {
            log.warn("User not confirmed: {}", email);
            throw new RuntimeException("Please confirm your email before logging in");
        } catch (Exception e) {
            log.error("Cognito login error for email: {}", email, e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Handle Cognito challenges (e.g., NEW_PASSWORD_REQUIRED)
     */
    private CognitoAuthResponse handleChallenge(InitiateAuthResponse authResponse, String email) {
        ChallengeNameType challengeName = authResponse.challengeName();

        if (challengeName == ChallengeNameType.NEW_PASSWORD_REQUIRED) {
            return CognitoAuthResponse.builder()
                    .requiresNewPassword(true)
                    .session(authResponse.session())
                    .challengeName(challengeName.toString())
                    .email(email)
                    .build();
        }

        // Handle other challenges as needed
        throw new RuntimeException("Unsupported challenge: " + challengeName);
    }

    /**
     * Respond to NEW_PASSWORD_REQUIRED challenge
     */
    public CognitoAuthResponse respondToNewPasswordChallenge(NewPasswordRequest request) {
        log.info("Responding to NEW_PASSWORD_REQUIRED challenge for: {}", request.getEmail());

        try {
            Map<String, String> challengeResponses = new HashMap<>();
            challengeResponses.put("USERNAME", request.getEmail());
            challengeResponses.put("NEW_PASSWORD", request.getNewPassword());

            if (cognitoProperties.getClientSecret() != null && !cognitoProperties.getClientSecret().isEmpty()) {
                challengeResponses.put("SECRET_HASH", calculateSecretHash(request.getEmail()));
            }

            RespondToAuthChallengeRequest challengeRequest = RespondToAuthChallengeRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                    .session(request.getSession())
                    .challengeResponses(challengeResponses)
                    .build();

            RespondToAuthChallengeResponse response = cognitoClient.respondToAuthChallenge(challengeRequest);
            AuthenticationResultType result = response.authenticationResult();

            // Publish login event
            publishLoginEvent(request.getEmail());

            return CognitoAuthResponse.builder()
                    .accessToken(result.accessToken())
                    .idToken(result.idToken())
                    .refreshToken(result.refreshToken())
                    .expiresIn(result.expiresIn())
                    .tokenType(result.tokenType())
                    .email(request.getEmail())
                    .build();

        } catch (Exception e) {
            log.error("Failed to respond to password challenge", e);
            throw new RuntimeException("Failed to set new password: " + e.getMessage());
        }
    }

    /**
     * Register a new user in Cognito
     */
    public CognitoAuthResponse register(RegisterDto registerDto) {
        log.info("Registering new user in Cognito: {}", registerDto.getEmail());

        try {
            List<AttributeType> userAttributes = new ArrayList<>();
            userAttributes.add(AttributeType.builder()
                    .name("email")
                    .value(registerDto.getEmail())
                    .build());
            userAttributes.add(AttributeType.builder()
                    .name("email_verified")
                    .value("true")
                    .build());

            // Add optional attributes
            if (registerDto.getFullName() != null) {
                userAttributes.add(AttributeType.builder()
                        .name("name")
                        .value(registerDto.getFullName())
                        .build());
            }
            if (registerDto.getPhone() != null) {
                userAttributes.add(AttributeType.builder()
                        .name("phone_number")
                        .value(registerDto.getPhone())
                        .build());
            }

            SignUpRequest.Builder signUpBuilder = SignUpRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(registerDto.getEmail())
                    .password(registerDto.getPassword())
                    .userAttributes(userAttributes);

            // Add secret hash if configured
            if (cognitoProperties.getClientSecret() != null && !cognitoProperties.getClientSecret().isEmpty()) {
                signUpBuilder.secretHash(calculateSecretHash(registerDto.getEmail()));
            }

            SignUpResponse signUpResponse = cognitoClient.signUp(signUpBuilder.build());
            String userSub = signUpResponse.userSub();

            log.info("User registered in Cognito with sub: {}", userSub);

            // Auto-confirm user if needed (for development)
            if (!signUpResponse.userConfirmed()) {
                confirmUserAdmin(registerDto.getEmail());
            }

            // Publish registration event to Kafka
            publishRegisterEvent(userSub, registerDto);

            // Auto-login after registration
            return login(registerDto.getEmail(), registerDto.getPassword());

        } catch (UsernameExistsException e) {
            log.warn("User already exists: {}", registerDto.getEmail());
            throw new RuntimeException("User with this email already exists");
        } catch (InvalidPasswordException e) {
            log.warn("Invalid password: {}", e.getMessage());
            throw new RuntimeException("Password does not meet requirements: " + e.getMessage());
        } catch (Exception e) {
            log.error("Cognito registration error", e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Admin confirm user (bypasses email verification)
     */
    private void confirmUserAdmin(String email) {
        try {
            AdminConfirmSignUpRequest confirmRequest = AdminConfirmSignUpRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(email)
                    .build();

            cognitoClient.adminConfirmSignUp(confirmRequest);
            log.info("User confirmed via admin: {}", email);
        } catch (Exception e) {
            log.warn("Could not auto-confirm user: {}", e.getMessage());
            // Don't throw - user might need to confirm via email
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public CognitoAuthResponse refreshToken(String refreshToken, String email) {
        log.info("Refreshing token for user");

        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("REFRESH_TOKEN", refreshToken);

            if (cognitoProperties.getClientSecret() != null && !cognitoProperties.getClientSecret().isEmpty()) {
                authParams.put("SECRET_HASH", calculateSecretHash(email));
            }

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .clientId(cognitoProperties.getClientId())
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);
            AuthenticationResultType result = authResponse.authenticationResult();

            // Publish token refresh event
            publishTokenRefreshEvent(email);

            return CognitoAuthResponse.builder()
                    .accessToken(result.accessToken())
                    .idToken(result.idToken())
                    // Refresh token is not returned on refresh, keep the original
                    .refreshToken(refreshToken)
                    .expiresIn(result.expiresIn())
                    .tokenType(result.tokenType())
                    .email(email)
                    .build();

        } catch (NotAuthorizedException e) {
            log.warn("Refresh token invalid or expired");
            throw new RuntimeException("Refresh token invalid or expired");
        } catch (Exception e) {
            log.error("Token refresh error", e);
            throw new RuntimeException("Failed to refresh token: " + e.getMessage());
        }
    }

    /**
     * Sign out user from all devices (global sign out)
     */
    public void logout(String accessToken) {
        try {
            GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
                    .accessToken(accessToken)
                    .build();

            cognitoClient.globalSignOut(signOutRequest);
            log.info("User signed out globally");

        } catch (Exception e) {
            log.warn("Global sign out failed: {}", e.getMessage());
            // Don't throw - we'll still clear cookies
        }
    }

    /**
     * Calculate secret hash for Cognito client with secret
     */
    private String calculateSecretHash(String username) {
        try {
            String message = username + cognitoProperties.getClientId();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    cognitoProperties.getClientSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate secret hash", e);
        }
    }

    // ============== Kafka Event Publishing ==============

    private void publishLoginEvent(String email) {
        try {
            kafkaProducerService.publishUserLoginEvent(
                    LoginEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("USER_LOGIN")
                            .timestamp(LocalDateTime.now())
                            .source(serviceName)
                            .version("1.0")
                            .email(email)
                            .build());
        } catch (Exception e) {
            log.warn("Failed to publish login event", e);
        }
    }

    private void publishRegisterEvent(String userSub, RegisterDto registerDto) {
        try {
            kafkaProducerService.publishUserRegisteredEvent(
                    RegisterEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("USER_REGISTERED")
                            .timestamp(LocalDateTime.now())
                            .source(serviceName)
                            .version("1.0")
                            .email(registerDto.getEmail())
                            .fullName(registerDto.getFullName())
                            .phone(registerDto.getPhone())
                            .address(registerDto.getAddress())
                            .cognitoSub(userSub)
                            .build());
        } catch (Exception e) {
            log.warn("Failed to publish register event", e);
        }
    }

    private void publishTokenRefreshEvent(String email) {
        try {
            kafkaProducerService.publishTokenRefreshedEvent(
                    TokenRefreshEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("TOKEN_REFRESHED")
                            .timestamp(LocalDateTime.now())
                            .source(serviceName)
                            .version("1.0")
                            .email(email)
                            .build());
        } catch (Exception e) {
            log.warn("Failed to publish token refresh event", e);
        }
    }

    public void publishLogoutEvent(String email) {
        try {
            kafkaProducerService.publishUserLogoutEvent(
                    UserLogoutEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("USER_LOGOUT")
                            .timestamp(LocalDateTime.now())
                            .source(serviceName)
                            .version("1.0")
                            .email(email)
                            .build());
        } catch (Exception e) {
            log.warn("Failed to publish logout event", e);
        }
    }
}
