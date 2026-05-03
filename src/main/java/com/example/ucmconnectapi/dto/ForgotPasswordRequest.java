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
@Schema(description = "Forgot password request")
public class ForgotPasswordRequest {

    @Schema(description = "User email address", example = "student@ucm.sk")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
