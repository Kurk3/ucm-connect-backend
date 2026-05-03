package com.example.ucmconnectapi.dto.nested;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Nested DTO representing post/comment author information
 * Used in detail responses to provide author context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Author information")
public class AuthorDTO {

    @Schema(description = "Author UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Author name", example = "Adam Kurek")
    private String name;

    @Schema(description = "Author email", example = "adam@test.com")
    private String email;
}
