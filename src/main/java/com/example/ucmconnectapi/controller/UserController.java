package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.dto.ErrorResponse;
import com.example.ucmconnectapi.dto.UpdateUserRequest;
import com.example.ucmconnectapi.dto.UserDTO;
import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.service.UserService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Operation(
            summary = "Get current user profile",
            description = "Get authenticated user's profile information. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        logger.debug("Getting current user profile");

        User currentUser = userService.getCurrentAuthenticatedUser();

        UserDTO userDTO = new UserDTO(
                currentUser.getId(),
                currentUser.getName(),
                currentUser.getNickName(),
                currentUser.getEmail(),
                currentUser.getCognitoUserId(),
                currentUser.getEmailVerified(),
                currentUser.getTwoFactorEnabled(),
                currentUser.getRole().name(),
                currentUser.getCreatedAt(),
                currentUser.getUpdatedAt()
        );

        logger.debug("Current user profile retrieved: {}", currentUser.getEmail());
        return ResponseEntity.ok(userDTO);
    }

    @Operation(
            summary = "Update current user profile",
            description = "Update authenticated user's profile information. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        logger.info("Updating current user profile");

        User currentUser = userService.getCurrentAuthenticatedUser();
        User updatedUser = userService.updateUser(currentUser.getId(), request.getName());

        UserDTO userDTO = new UserDTO(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getNickName(),
                updatedUser.getEmail(),
                updatedUser.getCognitoUserId(),
                updatedUser.getEmailVerified(),
                updatedUser.getTwoFactorEnabled(),
                updatedUser.getRole().name(),
                updatedUser.getCreatedAt(),
                updatedUser.getUpdatedAt()
        );

        logger.info("User profile updated: {}", updatedUser.getEmail());
        return ResponseEntity.ok(userDTO);
    }

    @Operation(
            summary = "Delete current user account",
            description = "Permanently delete authenticated user's account from both Cognito and database. " +
                    "This also deletes all user's posts, comments, and likes. This action cannot be undone."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        logger.warn("User account deletion requested");

        User currentUser = userService.getCurrentAuthenticatedUser();
        userService.deleteUser(currentUser.getId());

        logger.warn("User account deleted: {} (ID: {})", currentUser.getEmail(), currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
