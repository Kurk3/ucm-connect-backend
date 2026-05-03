package com.example.ucmconnectapi.dto;

import com.example.ucmconnectapi.dto.nested.AuthorDTO;
import com.example.ucmconnectapi.dto.nested.FileInfoDTO;
import com.example.ucmconnectapi.dto.nested.StatsDTO;
import com.example.ucmconnectapi.dto.nested.SubjectInfoDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detailed DTO for Post entity with nested structure
 * Used for GET /posts/{id} endpoint to provide comprehensive post information
 *
 * Differs from PostDTO (used in lists) by having nested objects for better data organization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed post information with nested structure")
public class PostDetailDTO {

    @Schema(description = "Post UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Post title", example = "Data Structures Cheat Sheet")
    private String title;

    @Schema(description = "Post description", example = "Complete guide to trees and graphs")
    private String description;

    @Schema(description = "Post author information")
    private AuthorDTO author;

    @Schema(description = "Subject information")
    private SubjectInfoDTO subject;

    @Schema(description = "File information")
    private FileInfoDTO file;

    @Schema(description = "Engagement statistics")
    private StatsDTO stats;

    @Schema(description = "Is post published", example = "true")
    private Boolean isPublished;

    @Schema(description = "Creation timestamp", example = "2025-10-12T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2025-10-12T14:20:00")
    private LocalDateTime updatedAt;
}
