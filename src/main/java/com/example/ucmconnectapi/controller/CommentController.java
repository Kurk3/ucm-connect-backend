package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.dto.CommentDTO;
import com.example.ucmconnectapi.dto.CommentDetailDTO;
import com.example.ucmconnectapi.dto.CommentListResponse;
import com.example.ucmconnectapi.dto.ErrorResponse;
import com.example.ucmconnectapi.service.CommentService;
import com.example.ucmconnectapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Comment management
 * Handles CRUD operations for comments on posts
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Comments", description = "Endpoints for managing comments on posts")
@SecurityRequirement(name = "Bearer Authentication")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    @Autowired
    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    /**
     * Get all comments for a specific post
     */
    @GetMapping("/posts/{postId}/comments")
    @Operation(
            summary = "Get all comments for a post",
            description = "Retrieve paginated list of comments for a specific post"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved comments",
                    content = @Content(schema = @Schema(implementation = CommentListResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<CommentListResponse> getCommentsByPostId(
            @Parameter(description = "Post UUID", required = true)
            @PathVariable UUID postId,

            @Parameter(description = "Page number (default: 1)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Items per page (default: 50, max: 100)")
            @RequestParam(defaultValue = "50") int limit
    ) {
        CommentListResponse response = commentService.getCommentsByPostId(postId, page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get single comment by ID with nested structure
     */
    @GetMapping("/comments/{id}")
    @Operation(
            summary = "Get comment by ID",
            description = "Retrieve detailed information about a specific comment with nested structure (author, stats)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment found",
                    content = @Content(schema = @Schema(implementation = CommentDetailDTO.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found")
    })
    public ResponseEntity<CommentDetailDTO> getCommentById(
            @Parameter(description = "Comment UUID", required = true)
            @PathVariable UUID id
    ) {
        CommentDetailDTO comment = commentService.getCommentDetailById(id);
        return ResponseEntity.ok(comment);
    }

    /**
     * Create new comment on a post
     */
    @PostMapping("/posts/{postId}/comments")
    @Operation(
            summary = "Create new comment",
            description = "Add a comment to a post. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment created successfully",
                    content = @Content(schema = @Schema(implementation = CommentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CommentDTO> createComment(
            @Parameter(description = "Post UUID to comment on", required = true)
            @PathVariable UUID postId,

            @Parameter(description = "Comment data", required = true)
            @Valid @RequestBody CommentDTO commentDTO
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        CommentDTO createdComment = commentService.createComment(postId, commentDTO, currentUserId);
        return ResponseEntity.status(201).body(createdComment);
    }

    /**
     * Update existing comment
     */
    @PutMapping("/comments/{id}")
    @Operation(
            summary = "Update comment",
            description = "Update comment content. Only the comment owner can update it. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment updated successfully",
                    content = @Content(schema = @Schema(implementation = CommentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the comment owner",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CommentDTO> updateComment(
            @Parameter(description = "Comment UUID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "Updated comment data", required = true)
            @Valid @RequestBody CommentDTO commentDTO
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        CommentDTO updatedComment = commentService.updateComment(id, commentDTO, currentUserId);
        return ResponseEntity.ok(updatedComment);
    }

    /**
     * Delete comment
     */
    @DeleteMapping("/comments/{id}")
    @Operation(
            summary = "Delete comment",
            description = "Delete a comment. Only the comment owner can delete it. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the comment owner",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "Comment UUID", required = true)
            @PathVariable UUID id
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        commentService.deleteComment(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
