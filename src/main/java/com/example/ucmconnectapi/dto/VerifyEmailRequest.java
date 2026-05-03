package com.example.ucmconnectapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Email verification request")
public class VerifyEmailRequest {

    @Schema(description = "User email address", example = "student@ucm.sk")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(description = "Verification code received via email", example = "123456")
    @NotBlank(message = "Verification code is required")
    private String verificationCode;
}
