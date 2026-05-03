package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.dto.ErrorResponse;
import com.example.ucmconnectapi.dto.RoleUpdateRequest;
import com.example.ucmconnectapi.dto.UserDTO;
import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.exception.ForbiddenException;
import com.example.ucmconnectapi.service.CommentService;
import com.example.ucmconnectapi.service.PostService;
import com.example.ucmconnectapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin-only endpoints for user and role management")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Operation(
            summary = "Get all users (Admin only)",
            description = "Retrieve list of all users with their roles. Only admins can access this endpoint."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not an admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        logger.info("Admin requesting all users list");

        // Verify admin role
        if (!userService.isCurrentUserAdmin()) {
            logger.warn("Non-admin user attempted to access admin endpoint");
            throw new ForbiddenException("Only admins can view all users");
        }

        List<User> users = userService.getAllUsers();
        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        logger.info("Returning {} users", userDTOs.size());
        return ResponseEntity.ok(userDTOs);
    }

    @Operation(
            summary = "Update user role (Admin only)",
            description = "Change a user's role. Only admins can access this endpoint. " +
                    "Available roles: USER, MODERATOR, ADMIN"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid role value",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not an admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody RoleUpdateRequest request) {
        logger.info("Admin requesting role change for user: {} to {}", userId, request.getRole());

        UUID adminId = userService.getCurrentUserIdFromToken();
        userService.updateUserRole(userId, request.getRole(), adminId);

        // Fetch updated user
        User updatedUser = userService.getUserById(userId);
        UserDTO userDTO = convertToDTO(updatedUser);

        logger.info("Role updated for user {} to {}", userId, request.getRole());
        return ResponseEntity.ok(userDTO);
    }

    @Operation(
            summary = "Get user by ID (Admin only)",
            description = "Get detailed user information by ID. Only admins can access this endpoint."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not an admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID userId) {
        logger.info("Admin requesting user details for: {}", userId);

        // Verify admin role
        if (!userService.isCurrentUserAdmin()) {
            logger.warn("Non-admin user attempted to access admin endpoint");
            throw new ForbiddenException("Only admins can view user details");
        }

        User user = userService.getUserById(userId);
        UserDTO userDTO = convertToDTO(user);

        return ResponseEntity.ok(userDTO);
    }

    @Operation(summary = "Delete any post (Admin only)", description = "Admin can delete any post regardless of ownership.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Post deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not an admin"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID postId) {
        logger.info("Admin deleting post: {}", postId);
        UUID adminId = userService.getCurrentUserIdFromToken();
        postService.deletePost(postId, adminId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete any comment (Admin only)", description = "Admin can delete any comment regardless of ownership.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not an admin"),
            @ApiResponse(responseCode = "404", description = "Comment not found")
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        logger.info("Admin deleting comment: {}", commentId);
        UUID adminId = userService.getCurrentUserIdFromToken();
        commentService.deleteComment(commentId, adminId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete user (Admin only)", description = "Admin can delete any user account. All user's posts and comments will be cascade deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        logger.info("Admin deleting user: {}", userId);

        if (!userService.isCurrentUserAdmin()) {
            throw new ForbiddenException("Only admins can delete users");
        }

        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getNickName(),
                user.getEmail(),
                user.getCognitoUserId(),
                user.getEmailVerified(),
                user.getTwoFactorEnabled(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
