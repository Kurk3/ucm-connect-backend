package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.security.JwtAuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Test", description = "Test endpoints to verify security configuration")
public class HelloController {

    @Autowired
    private JwtAuthenticationUtil jwtUtil;

    @GetMapping("/hello")
    @Operation(
        summary = "Public endpoint",
        description = "This endpoint is publicly accessible without authentication"
    )
    public String hello() {
        return "Hello World! This is a public endpoint.";
    }

    @GetMapping("/protected")
    @Operation(
        summary = "Protected endpoint",
        description = "This endpoint requires JWT authentication",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public Map<String, Object> protectedEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "You are authenticated!");
        response.put("userId", jwtUtil.getCurrentUserId());
        response.put("email", jwtUtil.getCurrentUserEmail());
        response.put("username", jwtUtil.getCurrentUsername());
        return response;
    }
}
