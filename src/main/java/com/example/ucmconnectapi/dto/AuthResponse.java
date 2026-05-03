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
@Schema(description = "Authentication response with JWT tokens")
public class AuthResponse {

    @Schema(description = "Access token for API requests", example = "eyJraWQiOiJ...")
    private String accessToken;

    @Schema(description = "ID token containing user information", example = "eyJraWQiOiJ...")
    private String idToken;

    @Schema(description = "Refresh token for obtaining new access tokens", example = "eyJjdHki...")
    private String refreshToken;

    @Schema(description = "Token expiration time in seconds", example = "3600")
    private Integer expiresIn;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";
}
