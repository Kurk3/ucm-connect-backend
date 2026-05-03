package com.example.ucmconnectapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility class for extracting user information from JWT tokens
 */
@Component
public class JwtAuthenticationUtil {

    /**
     * Get the currently authenticated user's ID (Cognito sub)
     * @return User ID from JWT token, or null if not authenticated
     */
    public String getCurrentUserId() {
        return getJwtClaim("sub");
    }

    /**
     * Get the currently authenticated user's email
     * @return Email from JWT token, or null if not authenticated
     */
    public String getCurrentUserEmail() {
        return getJwtClaim("email");
    }

    /**
     * Get the currently authenticated user's username
     * @return Username from JWT token, or null if not authenticated
     */
    public String getCurrentUsername() {
        return getJwtClaim("cognito:username");
    }

    /**
     * Get a specific claim from the JWT token
     * @param claimName Name of the claim
     * @return Claim value, or null if not found
     */
    public String getJwtClaim(String claimName) {
        return getCurrentJwt()
            .map(jwt -> jwt.getClaimAsString(claimName))
            .orElse(null);
    }

    /**
     * Get the full JWT token from the security context
     * @return JWT token, or empty if not authenticated
     */
    public Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            return Optional.of((Jwt) authentication.getPrincipal());
        }

        return Optional.empty();
    }

    /**
     * Check if the current user is authenticated
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
