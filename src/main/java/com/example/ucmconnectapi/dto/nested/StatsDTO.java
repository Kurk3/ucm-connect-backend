package com.example.ucmconnectapi.dto.nested;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested DTO representing statistics/metrics
 * Used in detail responses to group engagement metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Engagement statistics")
public class StatsDTO {

    @Schema(description = "Number of likes", example = "42")
    private Integer likes;

    @Schema(description = "Number of comments", example = "15")
    private Integer comments;

    @Schema(description = "Number of views (placeholder for future feature)", example = "0")
    @Builder.Default
    private Integer views = 0;
}
