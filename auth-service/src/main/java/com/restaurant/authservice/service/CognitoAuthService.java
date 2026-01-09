package com.restaurant.authservice.service;

import com.restaurant.authservice.config.CognitoProperties;
import com.restaurant.authservice.dto.AuthDto;
import com.restaurant.authservice.dto.AuthFilter;
import com.restaurant.authservice.dto.CognitoAuthResponse;
import com.restaurant.authservice.dto.NewPasswordRequest;
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
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoAuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final CognitoProperties cognitoProperties;
    private final AuthProducerService kafkaProducerService;
    private final AuthFactory authFactory;

    @Value("${spring.application.name:auth-service}")
    private String serviceName;

    public CognitoAuthResponse login(String email, String password) {
        log.info("Cognito login attempt for email: {}", email);

        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);

            if (cognitoProperties.getClientSecret() != null && !cognitoProperties.getClientSecret().isEmpty()) {
                authParams.put("SECRET_HASH", calculateSecretHash(email));
            }

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .clientId(cognitoProperties.getClientId())
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            if (authResponse.challengeName() != null) {
                log.info("Cognito challenge received: {}", authResponse.challengeName());
                return handleChallenge(authResponse, email);
            }

            AuthenticationResultType result = authResponse.authenticationResult();
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

        throw new RuntimeException("Unsupported challenge: " + challengeName);
    }

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

    public CognitoAuthResponse register(RegisterDto registerDto) {
        log.info("Registering new user in Cognito: {}", registerDto.getEmail());

        try {
            List<AttributeType> userAttributes = new ArrayList<>();
            userAttributes.add(AttributeType.builder()
                    .name("email")
                    .value(registerDto.getEmail())
                    .build());

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

            if (cognitoProperties.getClientSecret() != null && !cognitoProperties.getClientSecret().isEmpty()) {
                signUpBuilder.secretHash(calculateSecretHash(registerDto.getEmail()));
            }

            SignUpResponse signUpResponse = cognitoClient.signUp(signUpBuilder.build());
            String userSub = signUpResponse.userSub();

            log.info("User registered in Cognito with sub: {}", userSub);

            boolean userConfirmed = signUpResponse.userConfirmed();

            createAuthEntity(userSub, registerDto.getEmail());
            publishRegisterEvent(userSub, registerDto);

            if (userConfirmed) {
                return login(registerDto.getEmail(), registerDto.getPassword());
            } else {
                return CognitoAuthResponse.builder()
                        .email(registerDto.getEmail())
                        .sub(userSub)
                        .requiresConfirmation(true)
                        .message("Registration successful. Please check your email to confirm your account before logging in.")
                        .build();
            }

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

    public void confirmSignUp(String email, String confirmationCode) {
        log.info("Confirming sign up for email: {}", email);

        try {
            ConfirmSignUpRequest.Builder confirmBuilder = ConfirmSignUpRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(email)
                    .confirmationCode(confirmationCode);

            if (cognitoProperties.getClientSecret() != null && !cognitoProperties.getClientSecret().isEmpty()) {
                confirmBuilder.secretHash(calculateSecretHash(email));
            }

            cognitoClient.confirmSignUp(confirmBuilder.build());
            log.info("User confirmed successfully: {}", email);

        } catch (CodeMismatchException e) {
            throw new RuntimeException("Invalid confirmation code. Please try again.");
        } catch (ExpiredCodeException e) {
            throw new RuntimeException("Confirmation code has expired. Please request a new one.");
        } catch (UserNotFoundException e) {
            throw new RuntimeException("User not found. Please register first.");
        } catch (Exception e) {
            log.error("Error confirming user: {}", email, e);
            throw new RuntimeException("Failed to confirm email: " + e.getMessage());
        }
    }

    public void resendConfirmationCode(String email) {
        log.info("Resending confirmation code to email: {}", email);

        try {
            ResendConfirmationCodeRequest.Builder resendBuilder = ResendConfirmationCodeRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(email);

            if (cognitoProperties.getClientSecret() != null && !cognitoProperties.getClientSecret().isEmpty()) {
                resendBuilder.secretHash(calculateSecretHash(email));
            }

            cognitoClient.resendConfirmationCode(resendBuilder.build());
            log.info("Confirmation code resent successfully to: {}", email);

        } catch (UserNotFoundException e) {
            throw new RuntimeException("User not found. Please register first.");
        } catch (LimitExceededException e) {
            throw new RuntimeException("Too many attempts. Please wait before requesting a new code.");
        } catch (Exception e) {
            log.error("Error resending confirmation code: {}", email, e);
            throw new RuntimeException("Failed to resend confirmation code: " + e.getMessage());
        }
    }

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

            publishTokenRefreshEvent(email);

            return CognitoAuthResponse.builder()
                    .accessToken(result.accessToken())
                    .idToken(result.idToken())
                    .refreshToken(refreshToken)
                    .expiresIn(result.expiresIn())
                    .tokenType(result.tokenType())
                    .email(email)
                    .build();

        } catch (NotAuthorizedException e) {
            throw new RuntimeException("Refresh token invalid or expired");
        } catch (Exception e) {
            log.error("Token refresh error", e);
            throw new RuntimeException("Failed to refresh token: " + e.getMessage());
        }
    }

    public void logout(String accessToken) {
        try {
            GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
                    .accessToken(accessToken)
                    .build();

            cognitoClient.globalSignOut(signOutRequest);
            log.info("User signed out globally");

        } catch (Exception e) {
            log.warn("Global sign out failed: {}", e.getMessage());
        }
    }

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

    private void createAuthEntity(String cognitoSub, String email) {
        try {
            AuthDto authDto = AuthDto.builder()
                    .cognitoSub(cognitoSub)
                    .email(email)
                    .password(null)
                    .isActive(true)
                    .role(AuthEntity.UserRole.USER)
                    .build();
            authFactory.create(authDto);
            log.info("Created AuthEntity in database with cognitoSub: {}", cognitoSub);
        } catch (Exception e) {
            log.error("Failed to create AuthEntity in database for sub: {}", cognitoSub, e);
        }
    }

    public AuthDto getAuthByEmail(String email) throws DataFactoryException, CacheException {
        AuthFilter filter = AuthFilter.builder().email(email).build();
        return authFactory.getModel(filter);
    }

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
                            .cognitoSub(userSub)
                            .email(registerDto.getEmail())
                            .fullName(registerDto.getFullName())
                            .phone(registerDto.getPhone())
                            .address(registerDto.getAddress())
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
