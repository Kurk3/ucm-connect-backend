package com.example.ucmconnectapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response")
public class ErrorResponse {

    @Schema(description = "Error type", example = "ValidationError")
    private String error;

    @Schema(description = "Error message", example = "Invalid email format")
    private String message;

    @Schema(description = "Timestamp", example = "2025-10-20T12:57:28.123456")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Request path", example = "/api/v1/auth/register")
    private String path;

    @Schema(description = "Validation errors details")
    private Map<String, String> details;

    // Constructor without details
    public ErrorResponse(String error, String message, LocalDateTime timestamp, int status, String path) {
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
        this.status = status;
        this.path = path;
    }
}
