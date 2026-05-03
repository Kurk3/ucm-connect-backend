package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.dto.ErrorResponse;
import com.example.ucmconnectapi.dto.PostDTO;
import com.example.ucmconnectapi.dto.PostDetailDTO;
import com.example.ucmconnectapi.dto.PostListResponse;
import com.example.ucmconnectapi.service.PostService;
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
 * REST Controller for Post management
 * Handles CRUD operations for posts
 */
@RestController
@RequestMapping("/api/v1/posts")
@Tag(name = "Posts", description = "Endpoints for managing study material posts")
@SecurityRequirement(name = "Bearer Authentication")
public class PostController {

    private final PostService postService;
    private final UserService userService;

    @Autowired
    public PostController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    /**
     * Get all posts with pagination and optional filters
     */
    @GetMapping
    @Operation(
            summary = "Get all posts",
            description = "Retrieve paginated list of posts with optional filtering by subject or user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved posts",
                    content = @Content(schema = @Schema(implementation = PostListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<PostListResponse> getAllPosts(
            @Parameter(description = "Page number (default: 1)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Items per page (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int limit,

            @Parameter(description = "Filter by subject ID (optional)")
            @RequestParam(required = false) UUID subjectId,

            @Parameter(description = "Filter by user ID (optional)")
            @RequestParam(required = false) UUID userId
    ) {
        PostListResponse response = postService.getAllPosts(page, limit, subjectId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get single post by ID with nested structure
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get post by ID",
            description = "Retrieve detailed information about a specific post with nested structure (author, subject, file, stats)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post found",
                    content = @Content(schema = @Schema(implementation = PostDetailDTO.class))),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<PostDetailDTO> getPostById(
            @Parameter(description = "Post UUID", required = true)
            @PathVariable UUID id
    ) {
        PostDetailDTO post = postService.getPostDetailById(id);
        return ResponseEntity.ok(post);
    }

    /**
     * Create new post
     */
    @PostMapping
    @Operation(
            summary = "Create new post",
            description = "Create a new study material post. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Post created successfully",
                    content = @Content(schema = @Schema(implementation = PostDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subject not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PostDTO> createPost(
            @Parameter(description = "Post data", required = true)
            @Valid @RequestBody PostDTO postDTO
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        PostDTO createdPost = postService.createPost(postDTO, currentUserId);
        return ResponseEntity.status(201).body(createdPost);
    }

    /**
     * Update existing post
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update post",
            description = "Update post title, description, or subject. Only the post owner can update. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post updated successfully",
                    content = @Content(schema = @Schema(implementation = PostDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the post owner",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post or subject not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PostDTO> updatePost(
            @Parameter(description = "Post UUID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "Updated post data", required = true)
            @Valid @RequestBody PostDTO postDTO
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        PostDTO updatedPost = postService.updatePost(id, postDTO, currentUserId);
        return ResponseEntity.ok(updatedPost);
    }

    /**
     * Delete post
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete post",
            description = "Delete a post. Only the post owner can delete it. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Post deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the post owner",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "Post UUID", required = true)
            @PathVariable UUID id
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        postService.deletePost(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
