package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.dto.ErrorResponse;
import com.example.ucmconnectapi.dto.LikeDTO;
import com.example.ucmconnectapi.service.LikeService;
import com.example.ucmconnectapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Like management
 * Handles like/unlike operations for posts and comments
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Likes", description = "Endpoints for managing likes on posts and comments")
@SecurityRequirement(name = "Bearer Authentication")
public class LikeController {

    private final LikeService likeService;
    private final UserService userService;

    @Autowired
    public LikeController(LikeService likeService, UserService userService) {
        this.likeService = likeService;
        this.userService = userService;
    }

    /**
     * Like a post
     */
    @PostMapping("/posts/{postId}/likes")
    @Operation(
            summary = "Like a post",
            description = "Add a like to a post. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Post liked successfully",
                    content = @Content(schema = @Schema(implementation = LikeDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Already liked this post",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LikeDTO> likePost(
            @Parameter(description = "Post UUID to like", required = true)
            @PathVariable UUID postId
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        LikeDTO like = likeService.likePost(postId, currentUserId);
        return ResponseEntity.status(201).body(like);
    }

    /**
     * Unlike a post
     */
    @DeleteMapping("/posts/{postId}/likes")
    @Operation(
            summary = "Unlike a post",
            description = "Remove a like from a post. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Like removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post not found or not liked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> unlikePost(
            @Parameter(description = "Post UUID to unlike", required = true)
            @PathVariable UUID postId
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        likeService.unlikePost(postId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Like a comment
     */
    @PostMapping("/comments/{commentId}/likes")
    @Operation(
            summary = "Like a comment",
            description = "Add a like to a comment. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment liked successfully",
                    content = @Content(schema = @Schema(implementation = LikeDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Already liked this comment",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LikeDTO> likeComment(
            @Parameter(description = "Comment UUID to like", required = true)
            @PathVariable UUID commentId
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        LikeDTO like = likeService.likeComment(commentId, currentUserId);
        return ResponseEntity.status(201).body(like);
    }

    /**
     * Unlike a comment
     */
    @DeleteMapping("/comments/{commentId}/likes")
    @Operation(
            summary = "Unlike a comment",
            description = "Remove a like from a comment. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Like removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found or not liked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> unlikeComment(
            @Parameter(description = "Comment UUID to unlike", required = true)
            @PathVariable UUID commentId
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        likeService.unlikeComment(commentId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all likes by current user
     */
    @GetMapping("/users/me/likes")
    @Operation(
            summary = "Get current user's likes",
            description = "Retrieve all posts and comments liked by the current user. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved likes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LikeDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<LikeDTO>> getUserLikes() {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        List<LikeDTO> likes = likeService.getUserLikes(currentUserId);
        return ResponseEntity.ok(likes);
    }
}
