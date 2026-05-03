package com.example.ucmconnectapi.dto;

import com.example.ucmconnectapi.dto.nested.AuthorDTO;
import com.example.ucmconnectapi.dto.nested.StatsDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detailed DTO for Comment entity with nested structure
 * Used for GET /comments/{id} endpoint to provide comprehensive comment information
 *
 * Differs from CommentDTO (used in lists) by having nested objects for better data organization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed comment information with nested structure")
public class CommentDetailDTO {

    @Schema(description = "Comment UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Post ID this comment belongs to", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID postId;

    @Schema(description = "Comment author information")
    private AuthorDTO author;

    @Schema(description = "Comment content", example = "Great study material!")
    private String content;

    @Schema(description = "Engagement statistics")
    private StatsDTO stats;

    @Schema(description = "Creation timestamp", example = "2025-10-12T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2025-10-12T14:20:00")
    private LocalDateTime updatedAt;
}
