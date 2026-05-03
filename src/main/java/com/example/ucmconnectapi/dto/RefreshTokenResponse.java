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
@Schema(description = "Response with refreshed access token")
public class RefreshTokenResponse {

    @Schema(description = "New access token for API requests", example = "eyJraWQiOiJ...")
    private String accessToken;

    @Schema(description = "New ID token containing user information", example = "eyJraWQiOiJ...")
    private String idToken;

    @Schema(description = "Token expiration time in seconds", example = "3600")
    private Integer expiresIn;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";
}
