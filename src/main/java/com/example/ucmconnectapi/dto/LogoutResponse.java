package com.example.ucmconnectapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after successful logout")
public class LogoutResponse {

    @Schema(description = "Success message", example = "Logged out successfully")
    private String message;

    @Schema(description = "Timestamp of logout", example = "2025-11-17T17:30:00Z")
    private String timestamp;
}
