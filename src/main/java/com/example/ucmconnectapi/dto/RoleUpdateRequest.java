package com.example.ucmconnectapi.dto;

import com.example.ucmconnectapi.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update user role")
public class RoleUpdateRequest {

    @NotNull(message = "Role is required")
    @Schema(description = "New role to assign", example = "MODERATOR", allowableValues = {"USER", "MODERATOR", "ADMIN"})
    private Role role;
}
