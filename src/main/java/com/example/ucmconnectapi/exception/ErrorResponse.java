package com.example.ucmconnectapi.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response structure for API
 */
@Schema(description = "Error response")
public class ErrorResponse {

    @Schema(description = "Error type", example = "ValidationError")
    private String error;

    @Schema(description = "Error message", example = "Invalid email format")
    private String message;

    @Schema(description = "Timestamp when error occurred")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Request path", example = "/api/v1/subjects")
    private String path;

    @Schema(description = "Validation details (optional)")
    private Map<String, String> details;

    // Constructors
    public ErrorResponse() {
    }

    public ErrorResponse(String error, String message, LocalDateTime timestamp, int status, String path, Map<String, String> details) {
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
        this.status = status;
        this.path = path;
        this.details = details;
    }

    public ErrorResponse(String error, String message, int status, String path) {
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.path = path;
    }

    public ErrorResponse(String error, String message, int status, String path, Map<String, String> details) {
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.path = path;
        this.details = details;
    }

    // Getters and Setters
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
}
