package com.example.ucmconnectapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request")
public class RegisterRequest {

    @Schema(description = "User email address (must be @ucm.sk)", example = "2825012@ucm.sk")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @jakarta.validation.constraints.Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@ucm\\.sk$",
            message = "Only UCM email addresses (@ucm.sk) are allowed"
    )
    private String email;

    @Schema(description = "User password (min 8 characters, must include uppercase, lowercase, digit and special character)", example = "Test1234!")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @jakarta.validation.constraints.Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit and one special character"
    )
    private String password;

    @Schema(description = "User full name", example = "Adam Kurek")
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Schema(description = "User nickname (used for login)", example = "adam.kurek")
    @NotBlank(message = "Nickname is required")
    @Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
    private String nickName;
}
