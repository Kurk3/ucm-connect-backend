package com.example.ucmconnectapi.dto.nested;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Nested DTO representing subject information
 * Used in detail responses to provide full subject context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subject information")
public class SubjectInfoDTO {

    @Schema(description = "Subject UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Subject name", example = "Computer Science")
    private String name;

    @Schema(description = "Subject description", example = "Algorithms and data structures")
    private String description;
}
