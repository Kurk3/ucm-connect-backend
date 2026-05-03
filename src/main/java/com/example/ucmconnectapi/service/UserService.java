package com.example.ucmconnectapi.service;

import com.example.ucmconnectapi.entity.Role;
import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.exception.ForbiddenException;
import com.example.ucmconnectapi.exception.ResourceNotFoundException;
import com.example.ucmconnectapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CognitoService cognitoService;

    /**
     * Find or create user in PostgreSQL from Cognito JWT token
     * This syncs Cognito users with our local database
     */
    @Transactional
    public User syncUserFromCognito(String cognitoSub, String email, String name, String nickName) {
        return syncUserFromCognito(cognitoSub, email, name, nickName, false);
    }

    /**
     * Find or create user in PostgreSQL from Cognito
     * @param emailVerified Whether the email is already verified (true for JWT-based sync, false for registration)
     */
    @Transactional
    public User syncUserFromCognito(String cognitoSub, String email, String name, String nickName, boolean emailVerified) {
        // Check if user already exists by Cognito sub
        Optional<User> existingUser = userRepository.findByCognitoUserId(cognitoSub);

        if (existingUser.isPresent()) {
            logger.debug("User already exists in database: {}", email);
            return existingUser.get();
        }

        // Create new user
        User newUser = new User();
        newUser.setCognitoUserId(cognitoSub);
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setNickName(nickName);
        newUser.setEmailVerified(emailVerified); // Set based on context
        newUser.setTwoFactorEnabled(false);

        User savedUser = userRepository.save(newUser);
        logger.info("New user synced from Cognito to database: {}", email);
        return savedUser;
    }

    /**
     * Get user ID from current JWT token (works with both access token and ID token)
     * Only requires 'sub' claim which is present in all Cognito tokens
     */
    public UUID getCurrentUserIdFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        // Get JWT token from authentication
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract Cognito user ID (sub claim) - present in all token types
        String cognitoSub = jwt.getSubject();

        if (cognitoSub == null) {
            throw new IllegalStateException("Invalid JWT token: missing sub claim");
        }

        // Find user in database by Cognito sub
        User user = userRepository.findByCognitoUserId(cognitoSub)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Cognito ID: " + cognitoSub));

        return user.getId();
    }

    /**
     * Get currently authenticated user from Spring Security context
     * Works with both access tokens and ID tokens by fetching user from database
     * Only requires 'sub' claim which is present in all Cognito tokens
     */
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        // Get JWT token from authentication
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract Cognito user ID (sub claim) - present in all token types
        String cognitoSub = jwt.getSubject();

        if (cognitoSub == null) {
            throw new IllegalStateException("Invalid JWT token: missing sub claim");
        }

        // Find user in database by Cognito sub (single source of truth)
        User user = userRepository.findByCognitoUserId(cognitoSub)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Cognito ID: " + cognitoSub));

        logger.debug("Retrieved authenticated user: {} (ID: {})", user.getEmail(), user.getId());
        return user;
    }

    /**
     * Get user by ID (cached for 1 hour)
     */
    @Cacheable(value = "userById", key = "#userId")
    public User getUserById(UUID userId) {
        logger.debug("⚠️ CACHE MISS - Fetching user {} from database", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Update user profile (invalidates user cache)
     */
    @CacheEvict(value = "userById", key = "#userId")
    @Transactional
    public User updateUser(UUID userId, String newName) {
        // Fetch directly from repository to avoid cache issues
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        user.setName(newName);
        User updatedUser = userRepository.save(user);
        logger.info("User profile updated: {} - cache invalidated", userId);
        return updatedUser;
    }

    /**
     * Get user by email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Check if email already exists (for validation)
     */
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Mark user's email as verified in local database
     * Called after successful email verification in Cognito
     */
    @Transactional
    public void markEmailAsVerified(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEmailVerified(true);
            userRepository.save(user);
            logger.info("Email verification status updated in database for: {}", email);
        } else {
            logger.warn("Attempted to verify email for non-existent user: {}", email);
        }
    }

    /**
     * Evict user cache on logout
     * Clears cached user data to prevent stale data after logout
     */
    @CacheEvict(value = "userById", key = "#userId")
    public void evictUserCache(UUID userId) {
        logger.info("User cache evicted for user ID: {}", userId);
    }

    /**
     * Delete user from BOTH Cognito and PostgreSQL (invalidates user cache)
     * This ensures no orphaned accounts
     *
     * IMPORTANT: This also cascades to delete all user's posts, comments, and likes
     * due to ON DELETE CASCADE foreign key constraints
     */
    @CacheEvict(value = "userById", key = "#userId")
    @Transactional
    public void deleteUser(UUID userId) {
        // Fetch directly from repository to avoid cache issues
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        String email = user.getEmail();

        // Step 1: Delete from PostgreSQL (cascades to posts, comments, likes)
        userRepository.delete(user);
        logger.info("User deleted from database: {} (ID: {}) - cache invalidated", email, userId);

        // Step 2: Delete from Cognito
        cognitoService.deleteUser(email);
        logger.info("User fully deleted from both systems: {}", email);
    }

    // ==================== Role Management Methods ====================

    /**
     * Check if current authenticated user is an admin
     */
    public boolean isCurrentUserAdmin() {
        return getCurrentAuthenticatedUser().isAdmin();
    }

    /**
     * Check if current authenticated user is admin or moderator
     */
    public boolean isCurrentUserAdminOrModerator() {
        return getCurrentAuthenticatedUser().isAdminOrModerator();
    }

    /**
     * Update user role (Admin only)
     *
     * @param userId User to update
     * @param newRole New role to assign
     * @param adminId Admin performing the action
     * @throws ForbiddenException if current user is not an admin
     * @throws ResourceNotFoundException if user not found
     */
    @CacheEvict(value = "userById", key = "#userId")
    @Transactional
    public void updateUserRole(UUID userId, Role newRole, UUID adminId) {
        // Verify admin performing the action
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        if (!admin.isAdmin()) {
            logger.warn("User {} attempted to change roles without admin privileges", adminId);
            throw new ForbiddenException("Only admins can change user roles");
        }

        // Find target user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        logger.info("User {} role changed from {} to {} by admin {}", userId, oldRole, newRole, adminId);
    }

    /**
     * Get all users (Admin only)
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
