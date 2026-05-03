package com.example.ucmconnectapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Like entity
 * Used for responses (likes are created/deleted via URL parameters only)
 *
 * Note: All fields are response-only, no request body needed for likes
 */
@Schema(description = "Like details")
public class LikeDTO {

    @Schema(description = "Like UUID", example = "550e8400-e29b-41d4-a716-446655440000", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(description = "User ID who liked (comes from auth header)", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID userId;

    @Schema(description = "Post ID (if like is on a post, comes from URL)", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID postId;

    @Schema(description = "Comment ID (if like is on a comment, comes from URL)", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID commentId;

    @Schema(description = "Creation timestamp", example = "2025-10-12T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    // Constructors
    public LikeDTO() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public UUID getCommentId() {
        return commentId;
    }

    public void setCommentId(UUID commentId) {
        this.commentId = commentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
