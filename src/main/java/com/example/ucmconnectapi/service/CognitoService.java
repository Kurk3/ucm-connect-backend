package com.example.ucmconnectapi.service;

import com.example.ucmconnectapi.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class CognitoService {

    private static final Logger logger = LoggerFactory.getLogger(CognitoService.class);

    @Value("${aws.cognito.region}")
    private String region;

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    @Value("${aws.cognito.clientId}")
    private String clientId;

    @Value("${aws.cognito.clientSecret}")
    private String clientSecret;

    @Value("${aws.access.key.id}")
    private String accessKeyId;

    @Value("${aws.secret.access.key}")
    private String secretAccessKey;

    private CognitoIdentityProviderClient cognitoClient;

    @PostConstruct
    public void init() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
        logger.info("Cognito client initialized for region: {} and user pool: {}", region, userPoolId);
    }

    /**
     * Register a new user in Cognito
     */
    public SignUpResponse signUp(String email, String password, String name) {
        try {
            // Build user attributes
            AttributeType emailAttr = AttributeType.builder()
                    .name("email")
                    .value(email)
                    .build();

            AttributeType nameAttr = AttributeType.builder()
                    .name("name")
                    .value(name)
                    .build();

            // Create sign-up request - using email as Cognito username
            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(clientId)
                    .username(email)  // Using email as Cognito username
                    .password(password)
                    .userAttributes(emailAttr, nameAttr)
                    .secretHash(calculateSecretHash(email))
                    .build();

            SignUpResponse response = cognitoClient.signUp(signUpRequest);
            logger.info("User registered successfully: {}", email);
            return response;

        } catch (UsernameExistsException e) {
            logger.error("User already exists: {}", email);
            throw new ConflictException("User with this email already exists");
        } catch (InvalidPasswordException e) {
            logger.error("Invalid password for user: {}", email);
            throw new IllegalArgumentException("Password does not meet requirements: " + e.getMessage());
        } catch (CognitoIdentityProviderException e) {
            logger.error("Cognito error during sign-up: {}", e.getMessage());
            throw new RuntimeException("Failed to register user: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Authenticate user and return JWT tokens
     */
    public AuthenticationResultType initiateAuth(String email, String password) {
        try {
            // Prepare auth parameters - using email as Cognito username
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);
            authParams.put("SECRET_HASH", calculateSecretHash(email));

            // Create authentication request
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .clientId(clientId)
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse response = cognitoClient.initiateAuth(authRequest);
            logger.info("User authenticated successfully: {}", email);
            return response.authenticationResult();

        } catch (NotAuthorizedException e) {
            logger.error("Invalid credentials for user: {}", email);
            throw new IllegalArgumentException("Invalid email or password");
        } catch (UserNotConfirmedException e) {
            logger.error("User email not confirmed: {}", email);
            throw new IllegalArgumentException("Please verify your email before logging in");
        } catch (CognitoIdentityProviderException e) {
            logger.error("Cognito error during authentication: {}", e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Confirm user email with verification code
     */
    public void confirmSignUp(String email, String confirmationCode) {
        try {
            ConfirmSignUpRequest confirmRequest = ConfirmSignUpRequest.builder()
                    .clientId(clientId)
                    .username(email)  // Using email as Cognito username
                    .confirmationCode(confirmationCode)
                    .secretHash(calculateSecretHash(email))
                    .build();

            cognitoClient.confirmSignUp(confirmRequest);
            logger.info("Email confirmed successfully for user: {}", email);

        } catch (CodeMismatchException e) {
            logger.error("Invalid verification code for user: {}", email);
            throw new IllegalArgumentException("Invalid verification code");
        } catch (ExpiredCodeException e) {
            logger.error("Verification code expired for user: {}", email);
            throw new IllegalArgumentException("Verification code has expired. Please request a new one");
        } catch (CognitoIdentityProviderException e) {
            logger.error("Cognito error during email confirmation: {}", e.getMessage());
            throw new RuntimeException("Email confirmation failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Create user as admin (for test data seeding)
     * Creates pre-verified user with permanent password
     *
     * @param email User email (used as Cognito username)
     * @param password Permanent password
     * @param name User's display name
     * @return Cognito user ID (sub)
     */
    public String adminCreateUser(String email, String password, String name) {
        try {
            // Build user attributes
            AttributeType emailAttr = AttributeType.builder()
                    .name("email")
                    .value(email)
                    .build();

            AttributeType emailVerifiedAttr = AttributeType.builder()
                    .name("email_verified")
                    .value("true")  // Pre-verify email for test users
                    .build();

            AttributeType nameAttr = AttributeType.builder()
                    .name("name")
                    .value(name)
                    .build();

            // Create admin user request
            AdminCreateUserRequest createRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)  // Using email as Cognito username
                    .userAttributes(emailAttr, emailVerifiedAttr, nameAttr)
                    .messageAction(MessageActionType.SUPPRESS)  // Don't send welcome email
                    .temporaryPassword(password)  // Set temporary password first
                    .build();

            AdminCreateUserResponse createResponse = cognitoClient.adminCreateUser(createRequest);
            logger.info("User created in Cognito (admin): {}", email);

            // Set permanent password (override temporary password requirement)
            AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .password(password)
                    .permanent(true)  // Make password permanent (no forced reset)
                    .build();

            cognitoClient.adminSetUserPassword(setPasswordRequest);
            logger.info("Permanent password set for user: {}", email);

            // Extract and return Cognito user ID (sub)
            String cognitoUserId = createResponse.user().attributes().stream()
                    .filter(attr -> "sub".equals(attr.name()))
                    .map(AttributeType::value)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Cognito user ID (sub) not found in response"));

            logger.info("Cognito user ID (sub) for {}: {}", email, cognitoUserId);
            return cognitoUserId;

        } catch (UsernameExistsException e) {
            // User already exists in Cognito - fetch their user ID
            logger.warn("User already exists in Cognito: {}", email);
            try {
                AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(email)
                        .build();

                AdminGetUserResponse getUserResponse = cognitoClient.adminGetUser(getUserRequest);
                String cognitoUserId = getUserResponse.userAttributes().stream()
                        .filter(attr -> "sub".equals(attr.name()))
                        .map(AttributeType::value)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Cognito user ID (sub) not found"));

                logger.info("Retrieved existing Cognito user ID for {}: {}", email, cognitoUserId);
                return cognitoUserId;

            } catch (CognitoIdentityProviderException ex) {
                logger.error("Failed to retrieve existing Cognito user: {}", email);
                throw new RuntimeException("User exists but failed to retrieve user ID: " + ex.awsErrorDetails().errorMessage());
            }

        } catch (CognitoIdentityProviderException e) {
            logger.error("Cognito error during admin user creation: {}", e.getMessage());
            throw new RuntimeException("Failed to create user in Cognito: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Global sign out - invalidates all tokens for the user
     * This will revoke all access tokens and refresh tokens
     *
     * @param accessToken User's current access token
     */
    public void globalSignOut(String accessToken) {
        try {
            GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
                    .accessToken(accessToken)
                    .build();

            cognitoClient.globalSignOut(signOutRequest);
            logger.info("User globally signed out successfully");

        } catch (NotAuthorizedException e) {
            logger.error("Invalid or expired access token for global sign out");
            throw new IllegalArgumentException("Invalid or expired access token");
        } catch (CognitoIdentityProviderException e) {
            logger.error("Cognito error during global sign out: {}", e.getMessage());
            throw new RuntimeException("Failed to sign out: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Refresh access token using refresh token
     * Returns new access token and ID token (refresh token remains the same)
     *
     * @param refreshToken User's refresh token from login
     * @param username User's email (needed for SECRET_HASH calculation)
     * @return New authentication result with fresh access token and ID token
     */
    public AuthenticationResultType refreshAccessToken(String refreshToken, String username) {
        try {
            // For app clients with client secret, REFRESH_TOKEN_AUTH requires proper setup
            // AWS Cognito refresh token flow
            Map<String, String> authParams = new HashMap<>();
            authParams.put("REFRESH_TOKEN", refreshToken);
            authParams.put("SECRET_HASH", calculateSecretHash(username));

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .clientId(clientId)
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse response = cognitoClient.initiateAuth(authRequest);
            logger.info("Access token refreshed successfully for user: {}", username);
            return response.authenticationResult();

        } catch (NotAuthorizedException e) {
            logger.error("Invalid or expired refresh token for user: {} - AWS Error: {}",
                    username, e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : "Unknown");
            throw new IllegalArgumentException("Invalid or expired refresh token. Please log in again.");
        } catch (CognitoIdentityProviderException e) {
            logger.error("Cognito error during token refresh for user: {} - Error: {} - Details: {}",
                    username, e.getMessage(),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : "No details");
            throw new RuntimeException("Failed to refresh token: " +
                    (e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage()));
        }
    }

    /**
     * Initiate forgot password flow - sends verification code to user's email
     * Security: Always succeeds silently even if email doesn't exist (prevents email enumeration)
     */
    public void forgotPassword(String email) {
        try {
            software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest forgotRequest =
                    software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest.builder()
                            .clientId(clientId)
                            .username(email)
                            .secretHash(calculateSecretHash(email))
                            .build();

            cognitoClient.forgotPassword(forgotRequest);
            logger.info("Forgot password initiated for user: {}", email);

        } catch (UserNotFoundException e) {
            logger.warn("Forgot password requested for non-existent user: {}", email);
            // Security: Don't reveal if email exists - silently succeed
        } catch (InvalidParameterException e) {
            logger.warn("Forgot password for unverified user: {}", email);
            throw new IllegalArgumentException("Please verify your email address first before resetting password.");
        } catch (NotAuthorizedException e) {
            logger.warn("Forgot password not authorized for user: {}", email);
            // Cognito may throw this for unconfirmed users - silently succeed for security
        } catch (LimitExceededException e) {
            logger.error("Too many forgot password attempts for user: {}", email);
            throw new IllegalArgumentException("Too many attempts. Please try again later.");
        } catch (CognitoIdentityProviderException e) {
            logger.error("Cognito error during forgot password: {}", e.getMessage());
            throw new RuntimeException("Failed to initiate password reset: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Confirm forgot password - reset password using verification code
     */
    public void confirmForgotPassword(String email, String confirmationCode, String newPassword) {
        try {
            software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest confirmRequest =
                    software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest.builder()
                            .clientId(clientId)
                            .username(email)
                            .confirmationCode(confirmationCode)
                            .password(newPassword)
                            .secretHash(calculateSecretHash(email))
                            .build();

            cognitoClient.confirmForgotPassword(confirmRequest);
            logger.info("Password reset successfully for user: {}", email);

        } catch (CodeMismatchException e) {
            logger.error("Invalid reset code for user: {}", email);
            throw new IllegalArgumentException("Invalid verification code");
        } catch (ExpiredCodeException e) {
            logger.error("Reset code expired for user: {}", email);
            throw new IllegalArgumentException("Verification code has expired. Please request a new one");
        } catch (InvalidPasswordException e) {
            logger.error("Invalid new password for user: {}", email);
            throw new IllegalArgumentException("Password does not meet requirements");
        } catch (CognitoIdentityProviderException e) {
            logger.error("Cognito error during password reset: {}", e.getMessage());
            throw new RuntimeException("Password reset failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Delete user from Cognito (admin operation)
     * Used when user requests account deletion
     */
    public void deleteUser(String email) {
        try {
            AdminDeleteUserRequest deleteRequest = AdminDeleteUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)  // Using email as Cognito username
                    .build();

            cognitoClient.adminDeleteUser(deleteRequest);
            logger.info("User deleted from Cognito: {}", email);

        } catch (UserNotFoundException e) {
            logger.warn("User not found in Cognito during deletion: {}", email);
            // Don't throw exception - user might already be deleted
        } catch (CognitoIdentityProviderException e) {
            logger.error("Cognito error during user deletion: {}", e.getMessage());
            throw new RuntimeException("Failed to delete user from Cognito: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Delete ALL users from Cognito user pool
     * WARNING: This is a destructive operation! Use only for testing/cleanup.
     * @return Number of users deleted
     */
    public int deleteAllUsers() {
        logger.warn("DELETING ALL USERS FROM COGNITO USER POOL: {}", userPoolId);
        int deletedCount = 0;

        try {
            String paginationToken = null;

            do {
                // List users in Cognito user pool
                ListUsersRequest.Builder listRequestBuilder = ListUsersRequest.builder()
                        .userPoolId(userPoolId)
                        .limit(60); // Max allowed by AWS Cognito

                if (paginationToken != null) {
                    listRequestBuilder.paginationToken(paginationToken);
                }

                ListUsersRequest listRequest = listRequestBuilder.build();
                ListUsersResponse listResponse = cognitoClient.listUsers(listRequest);

                if (listResponse.users().isEmpty()) {
                    logger.info("No users found in Cognito user pool");
                    break;
                }

                // Delete each user
                for (UserType user : listResponse.users()) {
                    String username = user.username();
                    try {
                        AdminDeleteUserRequest deleteRequest = AdminDeleteUserRequest.builder()
                                .userPoolId(userPoolId)
                                .username(username)
                                .build();

                        cognitoClient.adminDeleteUser(deleteRequest);
                        deletedCount++;
                        logger.info("Deleted Cognito user: {}", username);
                    } catch (UserNotFoundException e) {
                        logger.warn("User not found (already deleted?): {}", username);
                    } catch (Exception e) {
                        logger.error("Failed to delete Cognito user: {}", username, e);
                        // Continue deleting other users even if one fails
                    }
                }

                // Get next page token
                paginationToken = listResponse.paginationToken();

            } while (paginationToken != null);

            logger.warn("Deleted {} users from Cognito user pool: {}", deletedCount, userPoolId);
            return deletedCount;

        } catch (CognitoIdentityProviderException e) {
            logger.error("Failed to list/delete users from Cognito", e);
            throw new RuntimeException("Failed to cleanup Cognito user pool: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Calculate SECRET_HASH for Cognito authentication
     * Required when app client has a secret configured
     */
    private String calculateSecretHash(String username) {
        try {
            String message = username + clientId;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    clientSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating secret hash", e);
        }
    }
}
