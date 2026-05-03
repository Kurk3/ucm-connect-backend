package com.example.ucmconnectapi.dto.nested;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested DTO representing file information
 * Used in detail responses to group file-related data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File information")
public class FileInfoDTO {

    @Schema(description = "File type", example = "pdf", allowableValues = {"pdf", "image", "zip"})
    private String type;

    @Schema(description = "File URL/S3 key", example = "550e8400_document.pdf")
    private String url;

    @Schema(description = "File size in bytes", example = "2048576")
    private Long size;

    @Schema(description = "Pre-signed download URL (temporary)", example = "https://s3.amazonaws.com/...")
    private String downloadUrl;
}
