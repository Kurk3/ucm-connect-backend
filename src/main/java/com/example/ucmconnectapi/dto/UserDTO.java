package com.example.ucmconnectapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile information")
public class UserDTO {

    @Schema(description = "User UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "User name", example = "Adam Kurek")
    private String name;

    @Schema(description = "User nickname", example = "adam.kurek")
    private String nickName;

    @Schema(description = "User email", example = "kurekadam314@gmail.com")
    private String email;

    @Schema(description = "Cognito user ID (sub claim)", example = "a1b2c3d4-5678-90ab-cdef-1234567890ab")
    private String cognitoUserId;

    @Schema(description = "Email verification status", example = "true")
    private Boolean emailVerified;

    @Schema(description = "Two-factor authentication enabled", example = "false")
    private Boolean twoFactorEnabled;

    @Schema(description = "User role", example = "USER", allowableValues = {"USER", "MODERATOR", "ADMIN"})
    private String role;

    @Schema(description = "Account creation timestamp", example = "2025-10-20T12:57:28.123456")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2025-10-20T13:15:42.987654")
    private LocalDateTime updatedAt;
}
