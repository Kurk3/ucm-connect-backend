package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.dto.*;
import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.service.CognitoService;
import com.example.ucmconnectapi.util.LogSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication endpoints (register, login, email verification)")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private CognitoService cognitoService;

    @Autowired
    private com.example.ucmconnectapi.service.UserService userService;

    @Operation(
            summary = "Register new user",
            description = "Create a new user account in AWS Cognito. User will receive email verification code."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "User already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration attempt for email: {}", LogSanitizer.sanitize(request.getEmail()));

        // Step 1: Register user in Cognito
        SignUpResponse signUpResponse = cognitoService.signUp(
                request.getEmail(),
                request.getPassword(),
                request.getName()
        );

        // Step 2: Create user in local database (eager sync)
        userService.syncUserFromCognito(
                signUpResponse.userSub(),  // Cognito user ID (sub)
                request.getEmail(),
                request.getName(),
                request.getNickName()
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration successful. Please check your email for verification code.");
        response.put("nickName", request.getNickName());
        response.put("email", request.getEmail());
        response.put("userSub", signUpResponse.userSub());

        logger.info("User registered successfully and synced to database: {}", LogSanitizer.sanitize(request.getEmail()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Verify email address",
            description = "Confirm user email with verification code sent via email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid verification code",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        logger.info("Email verification attempt for: {}", LogSanitizer.sanitize(request.getEmail()));

        // Step 1: Verify email in Cognito
        cognitoService.confirmSignUp(request.getEmail(), request.getVerificationCode());

        // Step 2: Update email_verified status in local database
        userService.markEmailAsVerified(request.getEmail());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Email verified successfully. You can now log in.");

        logger.info("Email verified successfully for: {}", LogSanitizer.sanitize(request.getEmail()));
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "User login",
            description = "Authenticate user and receive JWT tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or email not verified",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login attempt for email: {}", LogSanitizer.sanitize(request.getEmail()));

        AuthenticationResultType authResult = cognitoService.initiateAuth(
                request.getEmail(),
                request.getPassword()
        );

        AuthResponse response = AuthResponse.builder()
                .accessToken(authResult.accessToken())
                .idToken(authResult.idToken())
                .refreshToken(authResult.refreshToken())
                .expiresIn(authResult.expiresIn())
                .tokenType("Bearer")
                .build();

        logger.info("User logged in successfully: {}", LogSanitizer.sanitize(request.getEmail()));
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Logout user",
            description = "Global sign out - invalidates all active sessions and refresh tokens for the user",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully",
                    content = @Content(schema = @Schema(implementation = LogoutResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired access token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.info("Logout attempt");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(LogoutResponse.builder()
                    .message("Logged out successfully")
                    .timestamp(Instant.now().toString())
                    .build());
        }

        // Extract access token from Authorization header (remove "Bearer " prefix)
        String accessToken = authHeader.substring(7);

        // Get current user ID from token (works with access token which only has 'sub' claim)
        UUID userId = userService.getCurrentUserIdFromToken();

        // Step 1: Global sign out in Cognito (invalidates all tokens)
        cognitoService.globalSignOut(accessToken);

        // Step 2: Evict user cache
        userService.evictUserCache(userId);

        LogoutResponse response = LogoutResponse.builder()
                .message("Logged out successfully")
                .timestamp(Instant.now().toString())
                .build();

        logger.info("User logged out successfully and cache evicted for user ID: {}", userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Obtain new access token and ID token using refresh token. Refresh token remains valid."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        logger.info("Token refresh attempt for email: {}", LogSanitizer.sanitize(request.getEmail()));

        // Look up the Cognito username (sub UUID) — required for SECRET_HASH in REFRESH_TOKEN_AUTH flow
        User user = userService.getUserByEmail(request.getEmail());
        String cognitoUsername = user.getCognitoUserId();

        AuthenticationResultType authResult = cognitoService.refreshAccessToken(
                request.getRefreshToken(),
                cognitoUsername
        );

        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken(authResult.accessToken())
                .idToken(authResult.idToken())
                .expiresIn(authResult.expiresIn())
                .tokenType("Bearer")
                .build();

        logger.info("Access token refreshed successfully for: {}", LogSanitizer.sanitize(request.getEmail()));
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Request password reset",
            description = "Send password reset verification code to user's email. Always returns 200 for security (prevents email enumeration)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("Forgot password attempt for email: {}", LogSanitizer.sanitize(request.getEmail()));

        cognitoService.forgotPassword(request.getEmail());

        Map<String, String> response = new HashMap<>();
        response.put("message", "If an account with this email exists, a password reset code has been sent.");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reset password",
            description = "Reset user password using verification code received via email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid verification code or password",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        logger.info("Password reset attempt for email: {}", LogSanitizer.sanitize(request.getEmail()));

        cognitoService.confirmForgotPassword(
                request.getEmail(),
                request.getConfirmationCode(),
                request.getNewPassword()
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password has been reset successfully. You can now log in with your new password.");

        logger.info("Password reset successfully for: {}", LogSanitizer.sanitize(request.getEmail()));
        return ResponseEntity.ok(response);
    }
}
